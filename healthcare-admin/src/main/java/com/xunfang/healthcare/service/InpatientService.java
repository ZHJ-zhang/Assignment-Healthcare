package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.entity.BizAdmission;
import com.xunfang.healthcare.entity.BizBed;
import com.xunfang.healthcare.entity.BizCourseRecord;
import com.xunfang.healthcare.entity.BizMedicalOrder;
import com.xunfang.healthcare.entity.BizNursingRecord;
import com.xunfang.healthcare.entity.BizOrderExecution;
import com.xunfang.healthcare.entity.BizSettlement;
import com.xunfang.healthcare.mapper.BizAdmissionMapper;
import com.xunfang.healthcare.mapper.BizBedMapper;
import com.xunfang.healthcare.mapper.BizCourseRecordMapper;
import com.xunfang.healthcare.mapper.BizMedicalOrderMapper;
import com.xunfang.healthcare.mapper.BizNursingRecordMapper;
import com.xunfang.healthcare.mapper.BizOrderExecutionMapper;
import com.xunfang.healthcare.mapper.BizSettlementMapper;
import com.xunfang.healthcare.security.SecurityUtils;
import com.xunfang.healthcare.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 住院管理：床位、入院登记、医嘱、护理与病程、出院结算。
 */
@Service
@RequiredArgsConstructor
public class InpatientService {

    private final BizBedMapper bizBedMapper;
    private final BizAdmissionMapper bizAdmissionMapper;
    private final BizMedicalOrderMapper bizMedicalOrderMapper;
    private final BizOrderExecutionMapper bizOrderExecutionMapper;
    private final BizNursingRecordMapper bizNursingRecordMapper;
    private final BizCourseRecordMapper bizCourseRecordMapper;
    private final BizSettlementMapper bizSettlementMapper;
    private final BizNoGenerator bizNoGenerator;

    /**
     * 床位列表。
     */
    public List<BizBed> listBeds(Long deptId, Long wardId, Integer status) {
        return bizBedMapper.selectList(Wrappers.<BizBed>lambdaQuery()
                .eq(deptId != null, BizBed::getDeptId, deptId)
                .eq(wardId != null, BizBed::getWardId, wardId)
                .eq(status != null, BizBed::getStatus, status)
                .orderByAsc(BizBed::getWardId)
                .orderByAsc(BizBed::getBedNo));
    }

    /**
     * 保存床位。
     * <p>
     * 占用/维修状态只能由住院、转科、出院、维修等流程驱动，这里一律不接受手工指定，
     * 否则会把在住患者的床位直接改成空闲或维修，床位与住院记录就对不上了。
     */
    public BizBed saveBed(BizBed bed) {
        if (bed.getId() == null) {
            bed.setStatus(0);
            bed.setPatientId(null);
            bizBedMapper.insert(bed);
        } else {
            bed.setStatus(null);
            bed.setPatientId(null);
            bizBedMapper.updateById(bed);
        }
        return bed;
    }

    /**
     * 入院登记：占用床位。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizAdmission createAdmission(BizAdmission admission) {
        if (admission.getPatientId() == null) {
            throw BizException.badRequest("患者不能为空");
        }
        if (admission.getBedId() == null) {
            throw BizException.badRequest("请选择床位");
        }
        BizBed bed = bizBedMapper.selectById(admission.getBedId());
        if (bed == null) {
            throw BizException.badRequest("床位不存在");
        }
        int bedStatus = bed.getStatus() == null ? 0 : bed.getStatus();
        if (bedStatus == 1) {
            throw BizException.badRequest("该床位已被占用");
        }
        if (bedStatus == 2) {
            throw BizException.badRequest("该床位正在维修，不能安排患者");
        }

        admission.setId(null);
        admission.setAdmissionNo(bizNoGenerator.next("ZY"));
        if (admission.getAdmissionDate() == null) {
            admission.setAdmissionDate(LocalDate.now());
        }
        if (admission.getStatus() == null) {
            admission.setStatus(0);
        }
        if (admission.getDepositAmount() == null) {
            admission.setDepositAmount(BigDecimal.ZERO);
        }
        bizAdmissionMapper.insert(admission);

        BizBed bedUpdate = new BizBed();
        bedUpdate.setId(bed.getId());
        bedUpdate.setStatus(1);
        bedUpdate.setPatientId(admission.getPatientId());
        bizBedMapper.updateById(bedUpdate);
        return admission;
    }

    /**
     * 出院办理：释放床位。仅「在院」状态可办理，避免重复出院重复释放床位。
     */
    @Transactional(rollbackFor = Exception.class)
    public void discharge(Long id) {
        BizAdmission admission = requireAdmission(id);
        int status = admission.getStatus() == null ? 0 : admission.getStatus();
        if (status == 1) {
            throw BizException.badRequest("该患者已出院，请勿重复办理");
        }
        if (status != 0 && status != 2) {
            throw BizException.badRequest("该住院记录状态异常，无法办理出院");
        }
        BizAdmission update = new BizAdmission();
        update.setId(id);
        update.setStatus(1);
        update.setDischargeDate(LocalDate.now());
        bizAdmissionMapper.updateById(update);
        releaseBed(admission.getBedId());
    }

    /**
     * 转科：更换科室与床位。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long id, Long deptId, Long bedId) {
        BizAdmission admission = requireAdmission(id);
        int status = admission.getStatus() == null ? 0 : admission.getStatus();
        if (status == 1) {
            throw BizException.badRequest("该患者已出院，不能转科");
        }
        BizBed bed = bizBedMapper.selectById(bedId);
        if (bed == null) {
            throw BizException.badRequest("目标床位不存在");
        }
        int bedStatus = bed.getStatus() == null ? 0 : bed.getStatus();
        if (bedStatus == 1) {
            throw BizException.badRequest("该床位已被占用");
        }
        if (bedStatus == 2) {
            throw BizException.badRequest("该床位正在维修，不能转入");
        }
        // 先校验完目标床位再释放原床位，避免校验失败时原床已被腾空
        releaseBed(admission.getBedId());
        BizAdmission update = new BizAdmission();
        update.setId(id);
        update.setDeptId(deptId);
        update.setBedId(bedId);
        update.setStatus(2);
        bizAdmissionMapper.updateById(update);

        BizBed bedUpdate = new BizBed();
        bedUpdate.setId(bedId);
        bedUpdate.setStatus(1);
        bedUpdate.setPatientId(admission.getPatientId());
        bizBedMapper.updateById(bedUpdate);
    }

    /**
     * 住院记录分页。
     */
    public PageResult<BizAdmission> pageAdmissions(PageQuery query, Long deptId, Integer status,
                                                      Long patientId) {
        Page<BizAdmission> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizAdmissionMapper.selectPage(page, Wrappers.<BizAdmission>lambdaQuery()
                .eq(deptId != null, BizAdmission::getDeptId, deptId)
                .eq(status != null, BizAdmission::getStatus, status)
                .eq(patientId != null, BizAdmission::getPatientId, patientId)
                .ge(StrUtil.isNotBlank(query.getStartTime()),
                        BizAdmission::getAdmissionDate, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()),
                        BizAdmission::getAdmissionDate, query.getEndTime())
                .orderByDesc(BizAdmission::getCreateTime)));
    }

    /**
     * 医嘱分页。
     */
    public PageResult<BizMedicalOrder> pageOrders(PageQuery query, Long admissionId, Integer status) {
        Page<BizMedicalOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizMedicalOrderMapper.selectPage(page, Wrappers.<BizMedicalOrder>lambdaQuery()
                .eq(admissionId != null, BizMedicalOrder::getAdmissionId, admissionId)
                .eq(status != null, BizMedicalOrder::getStatus, status)
                .orderByDesc(BizMedicalOrder::getCreateTime)));
    }

    /**
     * 开具医嘱。
     */
    public BizMedicalOrder createOrder(BizMedicalOrder order) {
        if (order.getPatientId() == null) {
            throw BizException.badRequest("患者不能为空");
        }
        order.setId(null);
        order.setOrderNo(bizNoGenerator.next("YO"));
        if (order.getDoctorId() == null) {
            order.setDoctorId(SecurityUtils.getUserIdOrNull());
        }
        if (order.getDeptId() == null) {
            order.setDeptId(SecurityUtils.getDeptId());
        }
        if (order.getOrderType() == null) {
            order.setOrderType(1);
        }
        if (order.getStatus() == null) {
            order.setStatus(0);
        }
        if (order.getStartTime() == null) {
            order.setStartTime(LocalDateTime.now());
        }
        bizMedicalOrderMapper.insert(order);
        return order;
    }

    /**
     * 执行医嘱：已停止、已完成的医嘱不能再执行。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizOrderExecution executeOrder(Long id, String result) {
        BizMedicalOrder order = bizMedicalOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.badRequest("医嘱不存在");
        }
        int status = order.getStatus() == null ? 0 : order.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该医嘱已停止，不能再执行");
        }
        if (status == 3) {
            throw BizException.badRequest("该医嘱已完成，不能再执行");
        }
        BizMedicalOrder update = new BizMedicalOrder();
        update.setId(id);
        update.setStatus(1);
        bizMedicalOrderMapper.updateById(update);

        BizOrderExecution execution = new BizOrderExecution();
        execution.setOrderId(id);
        execution.setPatientId(order.getPatientId());
        execution.setExecuteTime(LocalDateTime.now());
        execution.setExecutorId(SecurityUtils.getUserIdOrNull());
        execution.setResult(result);
        execution.setStatus(1);
        bizOrderExecutionMapper.insert(execution);
        return execution;
    }

    /**
     * 完成医嘱：由「执行中」置为「已完成」，完成后的医嘱不再参与执行。
     */
    @Transactional(rollbackFor = Exception.class)
    public void finishOrder(Long id) {
        BizMedicalOrder order = bizMedicalOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.badRequest("医嘱不存在");
        }
        int status = order.getStatus() == null ? 0 : order.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该医嘱已停止，不能标记完成");
        }
        if (status == 3) {
            throw BizException.badRequest("该医嘱已完成，无需重复操作");
        }
        BizMedicalOrder update = new BizMedicalOrder();
        update.setId(id);
        update.setStatus(3);
        update.setEndTime(LocalDateTime.now());
        bizMedicalOrderMapper.updateById(update);
    }

    /**
     * 停止医嘱：已完成、已停止的医嘱不能再停止。
     */
    @Transactional(rollbackFor = Exception.class)
    public void stopOrder(Long id) {
        BizMedicalOrder order = bizMedicalOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.badRequest("医嘱不存在");
        }
        int status = order.getStatus() == null ? 0 : order.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该医嘱已停止，无需重复操作");
        }
        if (status == 3) {
            throw BizException.badRequest("该医嘱已完成，不能停止");
        }
        BizMedicalOrder update = new BizMedicalOrder();
        update.setId(id);
        update.setStatus(2);
        update.setEndTime(LocalDateTime.now());
        bizMedicalOrderMapper.updateById(update);
    }

    /**
     * 新增护理记录。
     */
    public BizNursingRecord addNursingRecord(BizNursingRecord record) {
        if (record.getPatientId() == null) {
            throw BizException.badRequest("患者不能为空");
        }
        record.setId(null);
        if (record.getNurseId() == null) {
            record.setNurseId(SecurityUtils.getUserIdOrNull());
        }
        if (record.getDeptId() == null) {
            record.setDeptId(SecurityUtils.getDeptId());
        }
        if (record.getRecordTime() == null) {
            record.setRecordTime(LocalDateTime.now());
        }
        if (record.getRecordType() == null) {
            record.setRecordType(1);
        }
        bizNursingRecordMapper.insert(record);
        return record;
    }

    /**
     * 护理记录分页。
     */
    public PageResult<BizNursingRecord> pageNursingRecords(PageQuery query, Long admissionId,
                                                            Long patientId) {
        Page<BizNursingRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizNursingRecordMapper.selectPage(page, Wrappers.<BizNursingRecord>lambdaQuery()
                .eq(admissionId != null, BizNursingRecord::getAdmissionId, admissionId)
                .eq(patientId != null, BizNursingRecord::getPatientId, patientId)
                .orderByDesc(BizNursingRecord::getRecordTime)));
    }

    /**
     * 新增住院病程记录。
     */
    public BizCourseRecord addCourseRecord(BizCourseRecord record) {
        if (record.getPatientId() == null) {
            throw BizException.badRequest("患者不能为空");
        }
        record.setId(null);
        if (record.getDoctorId() == null) {
            record.setDoctorId(SecurityUtils.getUserIdOrNull());
        }
        if (record.getDeptId() == null) {
            record.setDeptId(SecurityUtils.getDeptId());
        }
        if (record.getRecordTime() == null) {
            record.setRecordTime(LocalDateTime.now());
        }
        if (record.getRecordType() == null) {
            record.setRecordType(1);
        }
        bizCourseRecordMapper.insert(record);
        return record;
    }

    /**
     * 病程记录分页。
     */
    public PageResult<BizCourseRecord> pageCourseRecords(PageQuery query, Long admissionId,
                                                          Long patientId) {
        Page<BizCourseRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizCourseRecordMapper.selectPage(page, Wrappers.<BizCourseRecord>lambdaQuery()
                .eq(admissionId != null, BizCourseRecord::getAdmissionId, admissionId)
                .eq(patientId != null, BizCourseRecord::getPatientId, patientId)
                .orderByDesc(BizCourseRecord::getRecordTime)));
    }

    /**
     * 床位转维修：只有空闲的床位能挂维修，占用中要先出院或转床。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizBed maintainBed(Long id) {
        BizBed bed = requireBed(id);
        int status = bed.getStatus() == null ? 0 : bed.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该床位已在维修中");
        }
        if (status == 1) {
            throw BizException.badRequest("该床位有患者在住，请先办理出院或转床再维修");
        }
        bizBedMapper.update(null, Wrappers.<BizBed>lambdaUpdate()
                .eq(BizBed::getId, id)
                .set(BizBed::getStatus, 2)
                .set(BizBed::getPatientId, null));
        return bizBedMapper.selectById(id);
    }

    /**
     * 维修完成：床位恢复空闲，可重新安排患者。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizBed recoverBed(Long id) {
        BizBed bed = requireBed(id);
        int status = bed.getStatus() == null ? 0 : bed.getStatus();
        if (status != 2) {
            throw BizException.badRequest("只有维修中的床位才能恢复使用");
        }
        bizBedMapper.update(null, Wrappers.<BizBed>lambdaUpdate()
                .eq(BizBed::getId, id)
                .set(BizBed::getStatus, 0)
                .set(BizBed::getPatientId, null));
        return bizBedMapper.selectById(id);
    }

    private BizBed requireBed(Long id) {
        BizBed bed = bizBedMapper.selectById(id);
        if (bed == null) {
            throw BizException.badRequest("床位不存在");
        }
        return bed;
    }

    /**
     * 出院结算：同一住院单只能生成一张有效结算单，避免重复结算与重复退款。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizSettlement settle(Long admissionId, BigDecimal insuranceAmount, BigDecimal selfAmount) {
        BizAdmission admission = requireAdmission(admissionId);
        // 已退费的结算单不再视为有效，允许重新结算
        long settled = bizSettlementMapper.selectCount(Wrappers.<BizSettlement>lambdaQuery()
                .eq(BizSettlement::getAdmissionId, admissionId)
                .in(BizSettlement::getStatus, 0, 1));
        if (settled > 0) {
            throw BizException.badRequest("该住院单已结算，不能重复结算");
        }
        BigDecimal insurance = insuranceAmount == null ? BigDecimal.ZERO : insuranceAmount;
        BigDecimal self = selfAmount == null ? BigDecimal.ZERO : selfAmount;
        BizSettlement settlement = new BizSettlement();
        settlement.setSettlementNo(bizNoGenerator.next("JS"));
        settlement.setAdmissionId(admissionId);
        settlement.setPatientId(admission.getPatientId());
        settlement.setDeptId(admission.getDeptId());
        // 总费用 = 医保支付 + 个人自付；原先直接写成 selfAmount，漏掉了医保承担的部分
        settlement.setTotalAmount(insurance.add(self));
        settlement.setInsuranceAmount(insurance);
        settlement.setSelfAmount(self);
        settlement.setDepositAmount(admission.getDepositAmount() == null ? BigDecimal.ZERO : admission.getDepositAmount());
        // 应退押金 = 押金 - 个人自付（医保由医保基金结算，不从患者押金里扣）
        settlement.setRefundAmount(settlement.getDepositAmount().subtract(self));
        settlement.setStatus(1);
        settlement.setSettleTime(LocalDateTime.now());
        bizSettlementMapper.insert(settlement);
        return settlement;
    }

    /**
     * 结算退费：只有「已结算」的结算单能退，退了之后可以重新结算。
     * <p>
     * 这里只做账务上的冲销，不动住院单与床位——患者已经出院走了，
     * 床位该释放的早释放了，退费只是把钱退回去。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizSettlement refundSettlement(Long id, String reason) {
        BizSettlement settlement = bizSettlementMapper.selectById(id);
        if (settlement == null) {
            throw BizException.badRequest("结算单不存在");
        }
        int status = settlement.getStatus() == null ? 0 : settlement.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该结算单已退费，无需重复操作");
        }
        if (status != 1) {
            throw BizException.badRequest("只有已结算的结算单可以退费");
        }
        if (StrUtil.isBlank(reason)) {
            throw BizException.badRequest("请填写退费原因");
        }
        BizSettlement update = new BizSettlement();
        update.setId(id);
        update.setStatus(2);
        update.setRefundTime(LocalDateTime.now());
        update.setRefundReason(reason);
        bizSettlementMapper.updateById(update);
        return bizSettlementMapper.selectById(id);
    }

    /**
     * 结算记录分页。
     */
    public PageResult<BizSettlement> pageSettlements(PageQuery query, Long admissionId) {
        Page<BizSettlement> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizSettlementMapper.selectPage(page, Wrappers.<BizSettlement>lambdaQuery()
                .eq(admissionId != null, BizSettlement::getAdmissionId, admissionId)
                .orderByDesc(BizSettlement::getCreateTime)));
    }

    private void releaseBed(Long bedId) {
        if (bedId == null) {
            return;
        }
        // updateById 无法把字段置 null，必须走 lambdaUpdate 显式 set null
        bizBedMapper.update(null, Wrappers.<BizBed>lambdaUpdate()
                .eq(BizBed::getId, bedId)
                .set(BizBed::getStatus, 0)
                .set(BizBed::getPatientId, null));
    }

    private BizAdmission requireAdmission(Long id) {
        BizAdmission admission = bizAdmissionMapper.selectById(id);
        if (admission == null) {
            throw BizException.badRequest("住院记录不存在");
        }
        return admission;
    }
}
