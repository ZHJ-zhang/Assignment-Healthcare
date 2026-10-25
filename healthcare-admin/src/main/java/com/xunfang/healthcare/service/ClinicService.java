package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.PrescriptionStatus;
import com.xunfang.healthcare.dto.PrescriptionRequest;
import com.xunfang.healthcare.entity.BizMedicalRecord;
import com.xunfang.healthcare.entity.BizPrescription;
import com.xunfang.healthcare.entity.BizPrescriptionItem;
import com.xunfang.healthcare.entity.BizRegistration;
import com.xunfang.healthcare.mapper.BizMedicalRecordMapper;
import com.xunfang.healthcare.mapper.BizPrescriptionItemMapper;
import com.xunfang.healthcare.mapper.BizPrescriptionMapper;
import com.xunfang.healthcare.mapper.BizRegistrationMapper;
import com.xunfang.healthcare.security.DataScopeHelper;
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
 * 门诊：接诊、病历、处方。
 */
@Service
@RequiredArgsConstructor
public class ClinicService {

    private final BizRegistrationMapper bizRegistrationMapper;
    private final BizMedicalRecordMapper bizMedicalRecordMapper;
    private final BizPrescriptionMapper bizPrescriptionMapper;
    private final BizPrescriptionItemMapper bizPrescriptionItemMapper;
    private final BizNoGenerator bizNoGenerator;
    private final DataScopeHelper dataScopeHelper;

    /**
     * 医生工作台就诊列表：待就诊与就诊中的挂号。
     */
    public List<BizRegistration> listVisits(Long deptId, Integer status) {
        LambdaQueryWrapper<BizRegistration> wrapper = Wrappers.<BizRegistration>lambdaQuery()
                .eq(deptId != null, BizRegistration::getDeptId, deptId)
                .in(status != null, BizRegistration::getStatus, status)
                .in(status == null, BizRegistration::getStatus, 0, 1)
                .orderByAsc(BizRegistration::getQueueNo);
        // 医生的数据范围是"仅本人"：工作台上只看挂在自己名下的号
        dataScopeHelper.applySelf(wrapper, BizRegistration::getDoctorId);
        return bizRegistrationMapper.selectList(wrapper);
    }

    /**
     * 保存门诊病历。
     * <p>
     * 病历必须挂在一次就诊上（visitId），否则同一患者多次就诊的病历会搅在一起。
     * 前端没带 visitId 时，退化为取该患者当前就诊中的挂号，避免写出无法归属的病历。
     */
    public BizMedicalRecord saveRecord(BizMedicalRecord record) {
        if (record.getId() == null) {
            if (record.getPatientId() == null) {
                throw BizException.badRequest("患者不能为空");
            }
            if (record.getDoctorId() == null || dataScopeHelper.onlySelf()) {
                // 医生的数据范围是"仅本人"：病历只能记在自己名下
                record.setDoctorId(SecurityUtils.getUserIdOrNull());
            }
            if (record.getDeptId() == null || dataScopeHelper.onlySelf()) {
                record.setDeptId(SecurityUtils.getDeptId());
            }
            if (record.getVisitId() == null) {
                record.setVisitId(findCurrentVisitId(record.getPatientId()));
            }
            if (record.getVisitType() == null) {
                record.setVisitType(1);
            }
            if (record.getStatus() == null) {
                record.setStatus(0);
            }
            if (record.getVisitTime() == null) {
                record.setVisitTime(LocalDateTime.now());
            }
            bizMedicalRecordMapper.insert(record);
        } else {
            BizMedicalRecord existing = bizMedicalRecordMapper.selectById(record.getId());
            if (existing != null && existing.getStatus() != null && existing.getStatus() == 2) {
                throw BizException.badRequest("该病历已归档，不能再修改");
            }
            if (existing != null) {
                dataScopeHelper.checkOwner(existing.getDoctorId(), "病历");
            }
            bizMedicalRecordMapper.updateById(record);
        }
        return record;
    }

    /**
     * 取患者当前就诊中的挂号 ID。
     * <p>
     * 只认当天的挂号：昨天的号没点"完成就诊"会一直挂着 1，
     * 不限定日期的话今天的病历会挂到昨天那次就诊上。
     */
    private Long findCurrentVisitId(Long patientId) {
        if (patientId == null) {
            return null;
        }
        BizRegistration visit = bizRegistrationMapper.selectOne(Wrappers.<BizRegistration>lambdaQuery()
                .eq(BizRegistration::getPatientId, patientId)
                .eq(BizRegistration::getStatus, 1)
                .ge(BizRegistration::getRegTime, LocalDate.now().atStartOfDay())
                .orderByDesc(BizRegistration::getRegTime)
                .last("limit 1"));
        return visit == null ? null : visit.getId();
    }

    /**
     * 提交病历（草稿转已提交）。
     */
    public void submitRecord(Long id) {
        BizMedicalRecord record = bizMedicalRecordMapper.selectById(id);
        if (record == null) {
            throw BizException.badRequest("病历不存在");
        }
        dataScopeHelper.checkOwner(record.getDoctorId(), "病历");
        int status = record.getStatus() == null ? 0 : record.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该病历已归档，不能再提交");
        }
        if (status == 1) {
            throw BizException.badRequest("该病历已提交");
        }
        BizMedicalRecord update = new BizMedicalRecord();
        update.setId(id);
        update.setStatus(1);
        bizMedicalRecordMapper.updateById(update);
    }

    /**
     * 归档病历：已提交的病历才能归档，归档后不再允许修改。
     */
    @Transactional(rollbackFor = Exception.class)
    public void archiveRecord(Long id) {
        BizMedicalRecord record = bizMedicalRecordMapper.selectById(id);
        if (record == null) {
            throw BizException.badRequest("病历不存在");
        }
        int status = record.getStatus() == null ? 0 : record.getStatus();
        if (status == 2) {
            throw BizException.badRequest("该病历已归档，无需重复操作");
        }
        if (status != 1) {
            throw BizException.badRequest("只有已提交的病历可以归档，请先提交病历");
        }
        BizMedicalRecord update = new BizMedicalRecord();
        update.setId(id);
        update.setStatus(2);
        bizMedicalRecordMapper.updateById(update);
    }

    /**
     * 病历分页。
     */
    public PageResult<BizMedicalRecord> pageRecords(PageQuery query, Long patientId, Integer status, Long visitId) {
        Page<BizMedicalRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<BizMedicalRecord> wrapper = Wrappers.<BizMedicalRecord>lambdaQuery()
                .eq(patientId != null, BizMedicalRecord::getPatientId, patientId)
                .eq(status != null, BizMedicalRecord::getStatus, status)
                .eq(visitId != null, BizMedicalRecord::getVisitId, visitId)
                .orderByDesc(BizMedicalRecord::getVisitTime);
        // 医生只看自己写的病历
        dataScopeHelper.applySelf(wrapper, BizMedicalRecord::getDoctorId);
        return PageResult.of(bizMedicalRecordMapper.selectPage(page, wrapper));
    }

    /**
     * 开具处方。
     * <p>
     * 处方必须挂在一条「就诊中」的挂号上：没有接诊就开方等于跳步，
     * 会把待就诊甚至已退号的患者也开出药。医生与科室直接取自该挂号记录，
     * 不能用当前登录账号代替，否则处方上会出现「系统管理员」这种开方医生。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizPrescription createPrescription(PrescriptionRequest request) {
        if (request.getPatientId() == null) {
            throw BizException.badRequest("患者不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw BizException.badRequest("处方明细不能为空");
        }
        // 医生只能给自己名下的患者开方：不加这层过滤，任一医生都能替别人的患者开药
        Long scopeUserId = dataScopeHelper.scopeUserId();
        BizRegistration visit = bizRegistrationMapper.selectOne(Wrappers.<BizRegistration>lambdaQuery()
                .eq(BizRegistration::getPatientId, request.getPatientId())
                .eq(BizRegistration::getStatus, 1)
                .eq(scopeUserId != null, BizRegistration::getDoctorId, scopeUserId)
                .orderByDesc(BizRegistration::getRegTime)
                .last("limit 1"));
        if (visit == null) {
            throw BizException.badRequest(scopeUserId != null
                    ? "该患者没有挂在你名下，不能代其他医生开处方"
                    : "该患者当前没有就诊中的挂号，请先接诊后再开处方");
        }

        BizPrescription prescription = new BizPrescription();
        prescription.setPrescNo(bizNoGenerator.next("PR"));
        prescription.setPatientId(request.getPatientId());
        prescription.setDoctorId(visit.getDoctorId() == null ? SecurityUtils.getUserIdOrNull() : visit.getDoctorId());
        // 科室以挂号记录为准：患者挂的是哪个科，处方就属于哪个科，
        // 不能由前端传什么就写什么，否则医生可以把方子记到别的科室名下
        prescription.setDeptId(visit.getDeptId() != null ? visit.getDeptId() : request.getDeptId());
        prescription.setVisitId(visit.getId());
        prescription.setPrescType(request.getPrescType());
        // 医生开方即生效，直接进入「待收费」，收费台可立即检索到；不再有审核环节
        prescription.setStatus(PrescriptionStatus.PENDING_CHARGE);
        prescription.setRemark(request.getRemark());

        BigDecimal total = BigDecimal.ZERO;
        for (BizPrescriptionItem item : request.getItems()) {
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
            item.setId(null);
            item.setPrescriptionId(null);
            item.setQuantity(quantity);
            item.setPrice(price);
            item.setAmount(amount);
            total = total.add(amount);
        }
        prescription.setTotalAmount(total);
        bizPrescriptionMapper.insert(prescription);

        for (BizPrescriptionItem item : request.getItems()) {
            item.setPrescriptionId(prescription.getId());
            bizPrescriptionItemMapper.insert(item);
        }
        return prescription;
    }

    /**
     * 处方详情。
     */
    public BizPrescription prescriptionDetail(Long id) {
        BizPrescription prescription = bizPrescriptionMapper.selectById(id);
        if (prescription == null) {
            throw BizException.badRequest("处方不存在");
        }
        dataScopeHelper.checkOwner(prescription.getDoctorId(), "处方");
        return prescription;
    }

    /**
     * 处方明细分页。
     */
    public PageResult<BizPrescriptionItem> prescriptionItems(PageQuery query, Long prescriptionId) {
        Page<BizPrescriptionItem> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizPrescriptionItemMapper.selectPage(page, Wrappers.<BizPrescriptionItem>lambdaQuery()
                .eq(prescriptionId != null, BizPrescriptionItem::getPrescriptionId, prescriptionId)));
    }

    /**
     * 作废处方。
     * <p>
     * 只有「未收费」的处方能作废；作废后置为 5（已作废）而不是 3（已发药），
     * 否则药房会把这单当成待退药处方继续操作。
     */
    @Transactional(rollbackFor = Exception.class)
    public void voidPrescription(Long id) {
        BizPrescription prescription = bizPrescriptionMapper.selectById(id);
        if (prescription == null) {
            throw BizException.badRequest("处方不存在");
        }
        dataScopeHelper.checkOwner(prescription.getDoctorId(), "处方");
        int status = prescription.getStatus() == null ? 0 : prescription.getStatus();
        if (status >= PrescriptionStatus.PAID) {
            throw BizException.badRequest(status == PrescriptionStatus.CANCELLED
                    ? "该处方已作废"
                    : "已收费或已发药的处方不能作废，请走退费/退药流程");
        }
        BizPrescription update = new BizPrescription();
        update.setId(id);
        update.setStatus(PrescriptionStatus.CANCELLED);
        bizPrescriptionMapper.updateById(update);
        bizPrescriptionItemMapper.delete(Wrappers.<BizPrescriptionItem>lambdaQuery()
                .eq(BizPrescriptionItem::getPrescriptionId, id));
    }

    /**
     * 处方分页。
     */
    public PageResult<BizPrescription> pagePrescriptions(PageQuery query, Long patientId,
                                                          Integer status, Long doctorId, Long visitId) {
        Page<BizPrescription> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<BizPrescription> wrapper = Wrappers.<BizPrescription>lambdaQuery()
                .eq(patientId != null, BizPrescription::getPatientId, patientId)
                .eq(status != null, BizPrescription::getStatus, status)
                .eq(doctorId != null, BizPrescription::getDoctorId, doctorId)
                .eq(visitId != null, BizPrescription::getVisitId, visitId)
                .ge(StrUtil.isNotBlank(query.getStartTime()),
                        BizPrescription::getCreateTime, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()),
                        BizPrescription::getCreateTime, query.getEndTime() + " 23:59:59")
                .orderByDesc(BizPrescription::getCreateTime);
        // 医生只看自己开的处方，看不到其他科室和其他医生的
        dataScopeHelper.applySelf(wrapper, BizPrescription::getDoctorId);
        return PageResult.of(bizPrescriptionMapper.selectPage(page, wrapper));
    }
}
