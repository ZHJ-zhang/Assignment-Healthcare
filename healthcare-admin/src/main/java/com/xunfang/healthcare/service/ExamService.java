package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.ChargeItemType;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.entity.BizChargeItem;
import com.xunfang.healthcare.entity.BizCriticalValue;
import com.xunfang.healthcare.entity.BizExamApply;
import com.xunfang.healthcare.entity.BizExamReport;
import com.xunfang.healthcare.entity.BizPatient;
import com.xunfang.healthcare.entity.BizRegistration;
import com.xunfang.healthcare.mapper.BizChargeItemMapper;
import com.xunfang.healthcare.mapper.BizCriticalValueMapper;
import com.xunfang.healthcare.mapper.BizExamApplyMapper;
import com.xunfang.healthcare.mapper.BizExamReportMapper;
import com.xunfang.healthcare.mapper.BizPatientMapper;
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
 * 检查检验：申请单、检验报告、危急值。
 */
@Service
@RequiredArgsConstructor
public class ExamService {

    private final BizExamApplyMapper bizExamApplyMapper;
    private final BizExamReportMapper bizExamReportMapper;
    private final BizCriticalValueMapper bizCriticalValueMapper;
    private final BizChargeItemMapper bizChargeItemMapper;
    private final BizRegistrationMapper bizRegistrationMapper;
    private final BizPatientMapper bizPatientMapper;
    private final BizNoGenerator bizNoGenerator;
    private final NotifyService notifyService;
    private final DataScopeHelper dataScopeHelper;

    /**
     * 检查申请分页。
     */
    public PageResult<BizExamApply> pageApplies(PageQuery query, Integer examType, Integer status,
                                                 Long patientId) {
        Page<BizExamApply> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<BizExamApply> wrapper = Wrappers.<BizExamApply>lambdaQuery()
                .eq(examType != null, BizExamApply::getExamType, examType)
                .eq(status != null, BizExamApply::getStatus, status)
                .eq(patientId != null, BizExamApply::getPatientId, patientId)
                .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                        .like(BizExamApply::getApplyNo, query.getKeyword()).or()
                        .like(BizExamApply::getItemName, query.getKeyword()))
                .ge(StrUtil.isNotBlank(query.getStartTime()),
                        BizExamApply::getApplyTime, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()),
                        BizExamApply::getApplyTime, query.getEndTime() + " 23:59:59")
                .orderByDesc(BizExamApply::getApplyTime);
        // 医生只看自己开的检查单
        dataScopeHelper.applySelf(wrapper, BizExamApply::getDoctorId);
        return PageResult.of(bizExamApplyMapper.selectPage(page, wrapper));
    }

    /**
     * 开具检查申请单。
     * <p>
     * 申请单必须带上金额和就诊ID：金额是收费台生成检查费的依据，
     * 就诊ID决定这笔检查费归到哪一次就诊。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizExamApply createApply(BizExamApply apply) {
        if (apply.getPatientId() == null) {
            throw BizException.badRequest("患者不能为空");
        }
        apply.setId(null);
        apply.setApplyNo(bizNoGenerator.next("EX"));
        // 医生的数据范围是"仅本人"：申请单必须挂在自己名下，
        // 否则他给"别人"开一张单，自己反而在列表里看不到，单子成了孤儿
        if (dataScopeHelper.onlySelf() || apply.getDoctorId() == null) {
            apply.setDoctorId(SecurityUtils.getUserIdOrNull());
        }
        if (dataScopeHelper.onlySelf() || apply.getDeptId() == null) {
            apply.setDeptId(SecurityUtils.getDeptId());
        }
        if (apply.getVisitId() == null) {
            apply.setVisitId(findCurrentVisitId(apply.getPatientId()));
        }
        if (dataScopeHelper.onlySelf() && apply.getVisitId() == null) {
            throw BizException.badRequest("该患者今天没有挂在你名下，不能替他开检查单");
        }
        if (apply.getExamType() == null) {
            apply.setExamType(1);
        }
        if (apply.getAmount() == null) {
            apply.setAmount(BigDecimal.ZERO);
        }
        if (apply.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw BizException.badRequest("检查费金额不能为负数");
        }
        if (apply.getStatus() == null) {
            apply.setStatus(0);
        }
        if (apply.getApplyTime() == null) {
            apply.setApplyTime(LocalDateTime.now());
        }
        bizExamApplyMapper.insert(apply);
        return apply;
    }

    /**
     * 取患者当天在诊的挂号 ID。
     * <p>
     * 含「待就诊」：检查单允许在接诊前开（一期检查模块是独立页面），
     * 只认「就诊中」会让申请单落不到任何一次就诊上。
     */
    private Long findCurrentVisitId(Long patientId) {
        // 医生只能给挂在自己名下的患者开检查单
        Long scopeUserId = dataScopeHelper.scopeUserId();
        BizRegistration visit = bizRegistrationMapper.selectOne(Wrappers.<BizRegistration>lambdaQuery()
                .eq(BizRegistration::getPatientId, patientId)
                .in(BizRegistration::getStatus, 0, 1)
                .ge(BizRegistration::getRegTime, LocalDate.now().atStartOfDay())
                .eq(scopeUserId != null, BizRegistration::getDoctorId, scopeUserId)
                .orderByDesc(BizRegistration::getRegTime)
                .last("limit 1"));
        return visit == null ? null : visit.getId();
    }

    /**
     * 确认缴费：待缴费转为待采样。
     * <p>
     * 正常路径是收费台收费时自动推进状态，这里只是兜底。
     * 关键约束：没有已支付的收费明细就不许确认，否则等于不交钱也能做检查。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmPay(Long id) {
        BizExamApply apply = requireApply(id);
        int status = apply.getStatus() == null ? 0 : apply.getStatus();
        if (status >= 1) {
            // 收费台已经推进过了，重复确认按幂等处理
            return;
        }
        long paid = bizChargeItemMapper.selectCount(Wrappers.<BizChargeItem>lambdaQuery()
                .eq(BizChargeItem::getItemType, ChargeItemType.EXAM)
                .eq(BizChargeItem::getItemId, id));
        if (paid == 0) {
            throw BizException.badRequest("该申请单还没有收费记录，请先到收费台结清检查费");
        }
        BizExamApply update = new BizExamApply();
        update.setId(id);
        update.setStatus(1);
        bizExamApplyMapper.updateById(update);
    }

    /**
     * 样本采集：申请单需已确认缴费才能进入检验中状态。
     */
    public void collectSample(Long id) {
        BizExamApply apply = requireApply(id);
        int status = apply.getStatus() == null ? 0 : apply.getStatus();
        if (status == 0) {
            throw BizException.badRequest("该申请单尚未缴费，请先确认缴费再采样");
        }
        if (status != 1) {
            throw BizException.badRequest(status == 3 ? "该申请单已出报告，不能重复采样" : "该申请单已采样，无需重复操作");
        }
        BizExamApply update = new BizExamApply();
        update.setId(id);
        update.setStatus(2);
        update.setAuditorId(SecurityUtils.getUserIdOrNull());
        update.setAuditTime(LocalDateTime.now());
        bizExamApplyMapper.updateById(update);
    }

    /**
     * 检验报告分页。
     */
    public PageResult<BizExamReport> pageReports(PageQuery query, Long applyId, Integer status,
                                                 Long patientId) {
        Page<BizExamReport> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizExamReportMapper.selectPage(page, Wrappers.<BizExamReport>lambdaQuery()
                .eq(applyId != null, BizExamReport::getApplyId, applyId)
                .eq(status != null, BizExamReport::getStatus, status)
                .eq(patientId != null, BizExamReport::getPatientId, patientId)
                .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                        .like(BizExamReport::getReportTitle, query.getKeyword()).or()
                        .like(BizExamReport::getConclusion, query.getKeyword()))
                .orderByDesc(BizExamReport::getCreateTime)));
    }

    /**
     * 报告详情。
     */
    public BizExamReport reportDetail(Long id) {
        BizExamReport report = bizExamReportMapper.selectById(id);
        if (report == null) {
            throw BizException.badRequest("报告不存在");
        }
        return report;
    }

    /**
     * 录入结果：保存草稿或直接提交审核。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizExamReport saveReport(BizExamReport report) {
        if (report.getId() == null) {
            BizExamApply apply = bizExamApplyMapper.selectById(report.getApplyId());
            if (apply == null) {
                throw BizException.badRequest("申请单不存在");
            }
            report.setPatientId(apply.getPatientId());
            report.setStatus(0);
            if (report.getReportTime() == null) {
                report.setReportTime(LocalDateTime.now());
            }
            bizExamReportMapper.insert(report);

            // 申请单在报告审核通过时才推进到「已出报告」，草稿阶段仍停留在检验中

            if (report.getCriticalFlag() != null && report.getCriticalFlag() == 1) {
                BizCriticalValue critical = new BizCriticalValue();
                critical.setReportId(report.getId());
                critical.setPatientId(apply.getPatientId());
                critical.setDeptId(apply.getDeptId());
                critical.setItemName(apply.getItemName());
                critical.setValue(report.getConclusion());
                critical.setStatus(0);
                critical.setNotifyTime(LocalDateTime.now());
                bizCriticalValueMapper.insert(critical);
                notifyCriticalValue(apply, report, critical);
            }
        } else {
            bizExamReportMapper.updateById(report);
        }
        return report;
    }

    /**
     * 报告审核：仅草稿可审核，审核通过后申请单推进到「已出报告」。
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditReport(Long id) {
        BizExamReport report = reportDetail(id);
        int status = report.getStatus() == null ? 0 : report.getStatus();
        if (status == 1 || status == 2) {
            throw BizException.badRequest("该报告已审核，不能重复审核");
        }
        BizExamReport update = new BizExamReport();
        update.setId(id);
        update.setAuditorId(SecurityUtils.getUserIdOrNull());
        update.setAuditTime(LocalDateTime.now());
        update.setStatus(1);
        bizExamReportMapper.updateById(update);

        if (report.getApplyId() != null) {
            BizExamApply applyUpdate = new BizExamApply();
            applyUpdate.setId(report.getApplyId());
            applyUpdate.setStatus(3);
            bizExamApplyMapper.updateById(applyUpdate);
        }
    }

    /**
     * 报告发布。
     */
    public void publishReport(Long id) {
        BizExamReport report = reportDetail(id);
        if (report.getStatus() == null || report.getStatus() != 1) {
            throw BizException.badRequest("报告需先审核通过才能发布");
        }
        BizExamReport update = new BizExamReport();
        update.setId(id);
        update.setStatus(2);
        if (update.getReportTime() == null) {
            update.setReportTime(LocalDateTime.now());
        }
        bizExamReportMapper.updateById(update);
        notifyReportPublished(report);
    }

    /**
     * 报告发布：短信通知患者，给开单医生发站内通知。
     */
    private void notifyReportPublished(BizExamReport report) {
        BizPatient patient = report.getPatientId() == null ? null : bizPatientMapper.selectById(report.getPatientId());
        if (patient == null) {
            return;
        }
        String reportTitle = report.getReportTitle() == null ? "检查检验报告" : report.getReportTitle();
        notifyService.smsToPatient(patient, "REPORT_READY",
                patient.getName() + "您好，您的" + reportTitle + "已发布，请登录系统或到院查询详情。");

        BizExamApply apply = report.getApplyId() == null ? null : bizExamApplyMapper.selectById(report.getApplyId());
        Long doctorId = apply == null ? null : apply.getDoctorId();
        notifyService.inboxToUser(doctorId, "报告发布通知",
                "患者" + patient.getName() + "的" + reportTitle + "已发布，请及时查看。",
                1, String.valueOf(report.getId()));
    }

    /**
     * 危急值：给开单医生发预警提醒。
     */
    private void notifyCriticalValue(BizExamApply apply, BizExamReport report, BizCriticalValue critical) {
        BizPatient patient = apply.getPatientId() == null ? null : bizPatientMapper.selectById(apply.getPatientId());
        if (patient == null) {
            return;
        }
        String value = report.getConclusion() == null ? "异常" : report.getConclusion();
        notifyService.inboxToUser(apply.getDoctorId(), "危急值预警",
                "患者" + patient.getName() + "的" + apply.getItemName() + "结果为" + value + "，请及时处理。",
                3, String.valueOf(critical.getId()));
        // 通知已发出，状态由「待处理」推进到「已通知」，避免与处理完成混淆
        BizCriticalValue update = new BizCriticalValue();
        update.setId(critical.getId());
        update.setStatus(1);
        update.setNotifyTime(LocalDateTime.now());
        bizCriticalValueMapper.updateById(update);
    }

    /**
     * 危急值分页。
     */
    public PageResult<BizCriticalValue> pageCritical(PageQuery query, Integer status) {
        Page<BizCriticalValue> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizCriticalValueMapper.selectPage(page, Wrappers.<BizCriticalValue>lambdaQuery()
                .eq(status != null, BizCriticalValue::getStatus, status)
                .orderByDesc(BizCriticalValue::getCreateTime)));
    }

    /**
     * 危急值处理闭环。
     */
    public void handleCritical(Long id, String remark) {
        BizCriticalValue critical = bizCriticalValueMapper.selectById(id);
        if (critical == null) {
            throw BizException.badRequest("危急值记录不存在");
        }
        if (critical.getStatus() != null && critical.getStatus() == 2) {
            throw BizException.badRequest("该危急值已处理，无需重复操作");
        }
        BizCriticalValue update = new BizCriticalValue();
        update.setId(id);
        update.setStatus(2);
        update.setHandlerId(SecurityUtils.getUserIdOrNull());
        update.setHandleTime(LocalDateTime.now());
        update.setHandleRemark(remark);
        bizCriticalValueMapper.updateById(update);
    }

    private BizExamApply requireApply(Long id) {
        BizExamApply apply = bizExamApplyMapper.selectById(id);
        if (apply == null) {
            throw BizException.badRequest("检查申请单不存在");
        }
        return apply;
    }
}
