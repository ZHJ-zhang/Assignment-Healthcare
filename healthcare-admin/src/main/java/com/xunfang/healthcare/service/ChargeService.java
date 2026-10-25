package com.xunfang.healthcare.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.ChargeItemType;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.PrescriptionStatus;
import com.xunfang.healthcare.dto.ChargeRequest;
import com.xunfang.healthcare.entity.BizCharge;
import com.xunfang.healthcare.entity.BizChargeItem;
import com.xunfang.healthcare.entity.BizExamApply;
import com.xunfang.healthcare.entity.BizFeeItem;
import com.xunfang.healthcare.entity.BizInvoice;
import com.xunfang.healthcare.entity.BizPatient;
import com.xunfang.healthcare.entity.BizPrescription;
import com.xunfang.healthcare.entity.BizRefund;
import com.xunfang.healthcare.entity.BizRegistration;
import com.xunfang.healthcare.mapper.BizChargeItemMapper;
import com.xunfang.healthcare.mapper.BizChargeMapper;
import com.xunfang.healthcare.mapper.BizExamApplyMapper;
import com.xunfang.healthcare.mapper.BizFeeItemMapper;
import com.xunfang.healthcare.mapper.BizInvoiceMapper;
import com.xunfang.healthcare.mapper.BizPatientMapper;
import com.xunfang.healthcare.mapper.BizPrescriptionMapper;
import com.xunfang.healthcare.mapper.BizRefundMapper;
import com.xunfang.healthcare.mapper.BizRegistrationMapper;
import com.xunfang.healthcare.security.SecurityUtils;
import com.xunfang.healthcare.util.BizNoGenerator;
import com.xunfang.healthcare.vo.FinanceSummaryVO;
import com.xunfang.healthcare.vo.StatItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 收费、退费、发票与财务汇总。
 */
@Service
@RequiredArgsConstructor
public class ChargeService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BizChargeMapper bizChargeMapper;
    private final BizChargeItemMapper bizChargeItemMapper;
    private final BizFeeItemMapper bizFeeItemMapper;
    private final BizRefundMapper bizRefundMapper;
    private final BizInvoiceMapper bizInvoiceMapper;
    private final BizPatientMapper bizPatientMapper;
    private final BizPrescriptionMapper bizPrescriptionMapper;
    private final BizExamApplyMapper bizExamApplyMapper;
    private final BizRegistrationMapper bizRegistrationMapper;
    private final BizNoGenerator bizNoGenerator;

    /**
     * 收费：写入收费单与明细，金额以明细为准重新汇总。
     * <p>
     * 一次收费可以同时结清三种东西：手选的收费项目、一张处方、若干张检查申请单。
     * 它们都会落到同一张收费单上（由 visitId 归到同一次就诊），这样患者只需要排一次队。
     * <p>
     * 关联处方时按处方状态机校验，收费成功后把处方推进到 2（已收费），
     * 药房只有拿到 2 的处方才允许发药。检查申请同样在这里从 0（待缴费）推进到 1（待采样），
     * 不允许绕开收费台手工点"已缴费"。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizCharge createCharge(ChargeRequest request) {
        BizPrescription prescription = null;
        if (request.getPrescriptionId() != null) {
            prescription = requireChargeablePrescription(request.getPrescriptionId());
            if (request.getPatientId() == null) {
                request.setPatientId(prescription.getPatientId());
            }
            if (request.getDeptId() == null) {
                request.setDeptId(prescription.getDeptId());
            }
            if (request.getVisitId() == null) {
                request.setVisitId(prescription.getVisitId());
            }
        }
        BizPatient patient = bizPatientMapper.selectById(request.getPatientId());
        if (patient == null) {
            throw BizException.badRequest("患者不存在");
        }

        // 明细来源一：界面手选的收费项目
        List<BizChargeItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            items.addAll(request.getItems());
        }
        // 明细来源二：待缴费的检查申请单，按申请单上的金额自动生成明细
        List<BizExamApply> examApplies = new ArrayList<>();
        if (request.getExamApplyIds() != null && !request.getExamApplyIds().isEmpty()) {
            for (Long applyId : request.getExamApplyIds()) {
                BizExamApply apply = requirePayableExamApply(applyId, request.getPatientId());
                examApplies.add(apply);
                BizChargeItem examItem = new BizChargeItem();
                examItem.setItemType(ChargeItemType.EXAM);
                examItem.setItemId(apply.getId());
                examItem.setItemName(StrUtil.isBlank(apply.getItemName()) ? "检查费" : apply.getItemName());
                examItem.setQuantity(1);
                examItem.setPrice(apply.getAmount() == null ? BigDecimal.ZERO : apply.getAmount());
                items.add(examItem);
            }
        }
        if (items.isEmpty()) {
            throw BizException.badRequest("收费明细不能为空：请至少选择一个收费项目、处方或检查申请单");
        }
        // 科室与就诊号都跟着"本次就诊"走：前端没带就从当天最近的挂号里补，
        // 否则收费单会变成无科室、无就诊号的孤立单据。
        if (request.getVisitId() == null || request.getDeptId() == null) {
            BizRegistration visit = resolveCurrentVisit(request.getPatientId());
            if (visit != null) {
                if (request.getVisitId() == null) {
                    request.setVisitId(visit.getId());
                }
                if (request.getDeptId() == null) {
                    request.setDeptId(visit.getDeptId());
                }
            }
        }

        BizCharge charge = new BizCharge();
        charge.setChargeNo(bizNoGenerator.next("CH"));
        charge.setPatientId(request.getPatientId());
        charge.setDeptId(request.getDeptId());
        charge.setVisitId(request.getVisitId());
        charge.setPrescriptionId(request.getPrescriptionId());
        charge.setVisitType(request.getVisitType());
        charge.setPayType(request.getPayType());
        charge.setPayStatus(1);
        charge.setOperatorId(SecurityUtils.getUserIdOrNull());
        charge.setChargeTime(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (BizChargeItem item : items) {
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
            item.setAmount(amount);
            item.setQuantity(quantity);
            item.setPrice(price);
            total = total.add(amount);
        }
        BigDecimal discount = request.getDiscountAmount() == null ? BigDecimal.ZERO : request.getDiscountAmount();
        if (discount.compareTo(total) > 0) {
            throw BizException.badRequest("优惠金额不能超过总金额");
        }
        charge.setTotalAmount(total);
        charge.setDiscountAmount(discount);
        charge.setActualAmount(total.subtract(discount));
        bizChargeMapper.insert(charge);

        for (BizChargeItem item : items) {
            item.setId(null);
            item.setChargeId(charge.getId());
            bizChargeItemMapper.insert(item);
        }

        if (prescription != null) {
            BizPrescription prescUpdate = new BizPrescription();
            prescUpdate.setId(prescription.getId());
            prescUpdate.setStatus(PrescriptionStatus.PAID);
            bizPrescriptionMapper.updateById(prescUpdate);
        }
        // 检查申请：待缴费 → 待采样，采样环节才有依据
        for (BizExamApply apply : examApplies) {
            BizExamApply applyUpdate = new BizExamApply();
            applyUpdate.setId(apply.getId());
            applyUpdate.setStatus(1);
            bizExamApplyMapper.updateById(applyUpdate);
        }
        return charge;
    }

    /**
     * 挂号时自动生成挂号费收费单（已支付）。
     * <p>
     * 挂号费本来就是挂号当场收的钱，不能只记在挂号记录上再去收费台收一遍，
     * 否则财务报表里根本没有挂号费这一项。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizCharge createRegistrationFeeCharge(BizRegistration registration, BigDecimal fee, Integer payType) {
        if (fee == null || fee.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BizCharge charge = new BizCharge();
        charge.setChargeNo(bizNoGenerator.next("CH"));
        charge.setPatientId(registration.getPatientId());
        charge.setDeptId(registration.getDeptId());
        charge.setVisitId(registration.getId());
        charge.setVisitType(1);
        charge.setPayType(payType == null ? 1 : payType);
        charge.setPayStatus(1);
        charge.setTotalAmount(fee);
        charge.setDiscountAmount(BigDecimal.ZERO);
        charge.setActualAmount(fee);
        charge.setOperatorId(SecurityUtils.getUserIdOrNull());
        charge.setChargeTime(LocalDateTime.now());
        bizChargeMapper.insert(charge);

        BizChargeItem item = new BizChargeItem();
        item.setChargeId(charge.getId());
        item.setItemType(ChargeItemType.REGISTRATION);
        item.setItemId(registration.getId());
        item.setItemName("挂号费-" + regTypeName(registration.getRegType()));
        item.setQuantity(1);
        item.setPrice(fee);
        item.setAmount(fee);
        bizChargeItemMapper.insert(item);
        return charge;
    }

    /**
     * 退号时回收挂号费：把该次就诊的挂号费收费单置为已退费，并补一条已完成的退费单，账目可查。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundRegistrationFee(Long visitId) {
        if (visitId == null) {
            return;
        }
        List<BizCharge> charges = bizChargeMapper.selectList(Wrappers.<BizCharge>lambdaQuery()
                .eq(BizCharge::getVisitId, visitId)
                .eq(BizCharge::getVisitType, 1)
                .eq(BizCharge::getPayStatus, 1));
        for (BizCharge charge : charges) {
            BizCharge update = new BizCharge();
            update.setId(charge.getId());
            update.setPayStatus(2);
            bizChargeMapper.updateById(update);

            BizRefund refund = new BizRefund();
            refund.setRefundNo(bizNoGenerator.next("RF"));
            refund.setChargeId(charge.getId());
            refund.setPatientId(charge.getPatientId());
            refund.setRefundAmount(charge.getActualAmount());
            refund.setRefundType(1);
            refund.setStatus(2);
            refund.setReason("退号自动退挂号费");
            refund.setAuditorId(SecurityUtils.getUserIdOrNull());
            refund.setAuditTime(LocalDateTime.now());
            refund.setRefundTime(LocalDateTime.now());
            bizRefundMapper.insert(refund);
        }
    }

    /**
     * 校验检查申请单是否可收费：必须「待缴费」，属于同一患者，且没有被其他收费单收过。
     */
    private BizExamApply requirePayableExamApply(Long applyId, Long patientId) {
        BizExamApply apply = bizExamApplyMapper.selectById(applyId);
        if (apply == null) {
            throw BizException.badRequest("检查申请单不存在");
        }
        if (apply.getStatus() == null || apply.getStatus() != 0) {
            throw BizException.badRequest("申请单 " + apply.getApplyNo() + " 不是待缴费状态，不能重复收费");
        }
        if (patientId != null && !patientId.equals(apply.getPatientId())) {
            throw BizException.badRequest("申请单 " + apply.getApplyNo() + " 不属于当前患者");
        }
        long paid = bizChargeItemMapper.selectCount(Wrappers.<BizChargeItem>lambdaQuery()
                .eq(BizChargeItem::getItemType, ChargeItemType.EXAM)
                .eq(BizChargeItem::getItemId, applyId));
        if (paid > 0) {
            throw BizException.badRequest("申请单 " + apply.getApplyNo() + " 已生成过收费明细");
        }
        return apply;
    }

    /**
     * 未指定就诊ID时的兜底：取该患者当天还在诊的挂号。
     */
    private BizRegistration resolveCurrentVisit(Long patientId) {
        if (patientId == null) {
            return null;
        }
        return bizRegistrationMapper.selectOne(Wrappers.<BizRegistration>lambdaQuery()
                .eq(BizRegistration::getPatientId, patientId)
                .in(BizRegistration::getStatus, 0, 1)
                .ge(BizRegistration::getRegTime, LocalDate.now().atStartOfDay())
                .orderByDesc(BizRegistration::getRegTime)
                .last("limit 1"));
    }

    private String regTypeName(Integer regType) {
        if (regType == null) {
            return "普通号";
        }
        return switch (regType) {
            case 2 -> "专家号";
            case 3 -> "急诊号";
            default -> "普通号";
        };
    }

    /**
     * 校验处方是否允许收费：医生开方即生效，只要求「尚未收费」，且没有生成过有效收费单。
     */
    private BizPrescription requireChargeablePrescription(Long prescriptionId) {
        BizPrescription prescription = bizPrescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw BizException.badRequest("处方不存在");
        }
        int status = prescription.getStatus() == null
                ? PrescriptionStatus.PENDING_CHARGE : prescription.getStatus();
        switch (status) {
            case PrescriptionStatus.PENDING_CHARGE, PrescriptionStatus.LEGACY_AUDITED -> {
                // 待收费（含历史遗留的"已审核"），放行
            }
            case PrescriptionStatus.PAID -> throw BizException.badRequest("该处方已收费，请勿重复收费");
            case PrescriptionStatus.DISPENSED -> throw BizException.badRequest("该处方已发药，不能再次收费");
            case PrescriptionStatus.REFUNDED -> throw BizException.badRequest("该处方已退，不能收费");
            case PrescriptionStatus.CANCELLED -> throw BizException.badRequest("该处方已作废，不能收费");
            default -> throw BizException.badRequest("处方状态异常，无法收费");
        }
        long exists = bizChargeMapper.selectCount(Wrappers.<BizCharge>lambdaQuery()
                .eq(BizCharge::getPrescriptionId, prescriptionId)
                .eq(BizCharge::getPayStatus, 1));
        if (exists > 0) {
            throw BizException.badRequest("该处方已生成收费单，请勿重复收费");
        }
        return prescription;
    }

    /**
     * 收费单分页。
     */
    public PageResult<BizCharge> pageCharges(PageQuery query, Long patientId, Integer payStatus,
                                              Integer payType, Long deptId, Long visitId) {
        Page<BizCharge> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizChargeMapper.selectPage(page, Wrappers.<BizCharge>lambdaQuery()
                .eq(patientId != null, BizCharge::getPatientId, patientId)
                .eq(payStatus != null, BizCharge::getPayStatus, payStatus)
                .eq(payType != null, BizCharge::getPayType, payType)
                .eq(deptId != null, BizCharge::getDeptId, deptId)
                .eq(visitId != null, BizCharge::getVisitId, visitId)
                .like(StrUtil.isNotBlank(query.getKeyword()), BizCharge::getChargeNo, query.getKeyword())
                .ge(StrUtil.isNotBlank(query.getStartTime()), BizCharge::getChargeTime, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()), BizCharge::getChargeTime, query.getEndTime() + " 23:59:59")
                .orderByDesc(BizCharge::getChargeTime)));
    }

    /**
     * 收费详情：收费单 + 明细。
     */
    public Map<String, Object> chargeDetail(Long id) {
        BizCharge charge = bizChargeMapper.selectById(id);
        if (charge == null) {
            throw BizException.badRequest("收费单不存在");
        }
        List<BizChargeItem> items = bizChargeItemMapper.selectList(
                Wrappers.<BizChargeItem>lambdaQuery().eq(BizChargeItem::getChargeId, id));
        BizPatient patient = bizPatientMapper.selectById(charge.getPatientId());
        Map<String, Object> data = new HashMap<>(4);
        data.put("charge", charge);
        data.put("items", items);
        data.put("patient", patient);
        return data;
    }

    /**
     * 申请退费，状态置为待审核。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizRefund applyRefund(Long chargeId, BigDecimal amount, Integer refundType, String reason) {
        BizCharge charge = bizChargeMapper.selectById(chargeId);
        if (charge == null) {
            throw BizException.badRequest("收费单不存在");
        }
        if (charge.getPayStatus() == null || charge.getPayStatus() != 1) {
            throw BizException.badRequest("只有已支付的收费单可以退费");
        }
        BigDecimal refundAmount = amount == null ? charge.getActualAmount() : amount;
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("退费金额必须大于 0");
        }
        if (refundAmount.compareTo(charge.getActualAmount()) > 0) {
            throw BizException.badRequest("退费金额不能超过实收金额");
        }
        BizRefund refund = new BizRefund();
        refund.setRefundNo(bizNoGenerator.next("RF"));
        refund.setChargeId(chargeId);
        refund.setPatientId(charge.getPatientId());
        refund.setRefundAmount(refundAmount);
        refund.setRefundType(refundType == null ? 1 : refundType);
        refund.setStatus(0);
        refund.setReason(reason);
        bizRefundMapper.insert(refund);
        return refund;
    }

    /**
     * 审核退费：通过后直接把收费单标记为已退费。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizRefund auditRefund(Long id, boolean approved, String reason) {
        BizRefund refund = bizRefundMapper.selectById(id);
        if (refund == null) {
            throw BizException.badRequest("退费单不存在");
        }
        if (refund.getStatus() != null && refund.getStatus() != 0) {
            throw BizException.badRequest("该退费单已审核");
        }
        BizRefund update = new BizRefund();
        update.setId(id);
        update.setStatus(approved ? 1 : 3);
        update.setAuditorId(SecurityUtils.getUserIdOrNull());
        update.setAuditTime(LocalDateTime.now());
        if (!approved) {
            update.setReason(StrUtil.isBlank(reason) ? refund.getReason() : reason);
        }
        bizRefundMapper.updateById(update);
        if (approved) {
            // 部分退费时不能把整张收费单标记成已退：只有累计退掉的金额够上实收，
            // 收费单才转 2 已退，否则仍保持 1 已支付，剩余部分还能继续退。
            BizCharge charge = bizChargeMapper.selectById(refund.getChargeId());
            BigDecimal refunded = bizRefundMapper.selectList(Wrappers.<BizRefund>lambdaQuery()
                            .eq(BizRefund::getChargeId, refund.getChargeId())
                            .in(BizRefund::getStatus, 1, 2))
                    .stream()
                    .map(BizRefund::getRefundAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (charge != null && (charge.getActualAmount() == null
                    || refunded.compareTo(charge.getActualAmount()) >= 0)) {
                BizCharge chargeUpdate = new BizCharge();
                chargeUpdate.setId(refund.getChargeId());
                chargeUpdate.setPayStatus(2);
                bizChargeMapper.updateById(chargeUpdate);
                revertPrescriptionOnRefund(refund.getChargeId());
            }
        }
        return bizRefundMapper.selectById(id);
    }

    /**
     * 退费通过后回收处方状态：仅回收「已收费」的处方，
     * 已发药的处方要等药房退药流程处理，不能在这里直接置为已退。
     */
    private void revertPrescriptionOnRefund(Long chargeId) {
        BizCharge charge = bizChargeMapper.selectById(chargeId);
        if (charge == null || charge.getPrescriptionId() == null) {
            return;
        }
        BizPrescription prescription = bizPrescriptionMapper.selectById(charge.getPrescriptionId());
        if (prescription == null || prescription.getStatus() == null
                || prescription.getStatus() != PrescriptionStatus.PAID) {
            return;
        }
        BizPrescription prescUpdate = new BizPrescription();
        prescUpdate.setId(prescription.getId());
        prescUpdate.setStatus(PrescriptionStatus.REFUNDED);
        bizPrescriptionMapper.updateById(prescUpdate);
    }

    /**
     * 执行退费：审核通过后退款到账。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizRefund refund(Long id) {
        BizRefund refund = bizRefundMapper.selectById(id);
        if (refund == null) {
            throw BizException.badRequest("退费单不存在");
        }
        if (refund.getStatus() == null || refund.getStatus() != 1) {
            throw BizException.badRequest("退费单需先审核通过");
        }
        BizRefund update = new BizRefund();
        update.setId(id);
        update.setStatus(2);
        update.setRefundTime(LocalDateTime.now());
        bizRefundMapper.updateById(update);
        return bizRefundMapper.selectById(id);
    }

    /**
     * 退费单分页。
     */
    public PageResult<BizRefund> pageRefunds(PageQuery query, Integer status, Long patientId) {
        Page<BizRefund> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizRefundMapper.selectPage(page, Wrappers.<BizRefund>lambdaQuery()
                .eq(status != null, BizRefund::getStatus, status)
                .eq(patientId != null, BizRefund::getPatientId, patientId)
                .like(StrUtil.isNotBlank(query.getKeyword()), BizRefund::getRefundNo, query.getKeyword())
                .ge(StrUtil.isNotBlank(query.getStartTime()), BizRefund::getCreateTime, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()), BizRefund::getCreateTime, query.getEndTime() + " 23:59:59")
                .orderByDesc(BizRefund::getCreateTime)));
    }

    /**
     * 开具发票。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizInvoice printInvoice(Long chargeId, String title, String taxNo, Integer invoiceType) {
        BizCharge charge = bizChargeMapper.selectById(chargeId);
        if (charge == null) {
            throw BizException.badRequest("收费单不存在");
        }
        if (charge.getPayStatus() == null || charge.getPayStatus() != 1) {
            throw BizException.badRequest("只有已支付的收费单可以开票");
        }
        long exists = bizInvoiceMapper.selectCount(Wrappers.<BizInvoice>lambdaQuery()
                .eq(BizInvoice::getChargeId, chargeId)
                .eq(BizInvoice::getStatus, 1));
        if (exists > 0) {
            throw BizException.badRequest("该收费单已开票");
        }
        BizInvoice invoice = new BizInvoice();
        invoice.setInvoiceNo(bizNoGenerator.next("INV"));
        invoice.setInvoiceCode("01" + invoice.getInvoiceNo());
        invoice.setChargeId(chargeId);
        invoice.setPatientId(charge.getPatientId());
        invoice.setTitle(StrUtil.isBlank(title) ? "个人" : title);
        invoice.setTaxNo(taxNo);
        invoice.setAmount(charge.getActualAmount());
        invoice.setInvoiceType(invoiceType == null ? 1 : invoiceType);
        invoice.setStatus(1);
        invoice.setInvoiceTime(LocalDateTime.now());
        bizInvoiceMapper.insert(invoice);
        return invoice;
    }

    /**
     * 发票分页。
     */
    public PageResult<BizInvoice> pageInvoices(PageQuery query, Integer status, Long patientId) {
        Page<BizInvoice> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizInvoiceMapper.selectPage(page, Wrappers.<BizInvoice>lambdaQuery()
                .eq(status != null, BizInvoice::getStatus, status)
                .eq(patientId != null, BizInvoice::getPatientId, patientId)
                .like(StrUtil.isNotBlank(query.getKeyword()), BizInvoice::getInvoiceNo, query.getKeyword())
                .orderByDesc(BizInvoice::getCreateTime)));
    }

    /**
     * 作废发票。
     */
    @Transactional(rollbackFor = Exception.class)
    public BizInvoice voidInvoice(Long id, String reason) {
        BizInvoice invoice = bizInvoiceMapper.selectById(id);
        if (invoice == null) {
            throw BizException.badRequest("发票不存在");
        }
        if (invoice.getStatus() != null && invoice.getStatus() != 1) {
            throw BizException.badRequest("只有已开票的发票可以作废");
        }
        BizInvoice update = new BizInvoice();
        update.setId(id);
        update.setStatus(2);
        update.setVoidReason(reason);
        bizInvoiceMapper.updateById(update);
        return bizInvoiceMapper.selectById(id);
    }

    /**
     * 财务汇总：按日、支付方式、科室三个维度。
     */
    public FinanceSummaryVO summary(String startDate, String endDate) {
        LocalDate start = parseDay(startDate, LocalDate.now().withDayOfMonth(1));
        LocalDate end = parseDay(endDate, LocalDate.now());
        String startText = start.format(DAY);
        String endText = end.format(DAY) + " 23:59:59";

        FinanceSummaryVO vo = new FinanceSummaryVO();
        vo.setStartDate(startText);
        vo.setEndDate(end.format(DAY));

        List<StatItemVO> daily = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        long count = 0L;
        for (Map<String, Object> row : bizChargeMapper.selectDailySummary(startText, endText)) {
            BigDecimal amount = toDecimal(row.get("total_amount"));
            total = total.add(amount);
            count += toLong(row.get("total_count"));
            daily.add(new StatItemVO(toStr(row.get("stat_date")), amount));
        }
        vo.setTotalAmount(total);
        vo.setTotalCount(count);
        vo.setDailyList(daily);

        List<StatItemVO> payType = new ArrayList<>();
        for (Map<String, Object> row : bizChargeMapper.selectPayTypeSummary(startText, endText)) {
            payType.add(new StatItemVO(payTypeName(toInt(row.get("pay_type"))), toDecimal(row.get("total_amount"))));
        }
        vo.setPayTypeList(payType);

        List<StatItemVO> dept = new ArrayList<>();
        for (Map<String, Object> row : bizChargeMapper.selectDeptSummary(startText, endText)) {
            dept.add(new StatItemVO(toStr(row.get("dept_name")), toDecimal(row.get("total_amount"))));
        }
        vo.setDeptList(dept);
        return vo;
    }

    /**
     * 收费项目分页。
     */
    public PageResult<BizFeeItem> pageFeeItems(PageQuery query, Integer itemType, Integer status) {
        Page<BizFeeItem> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(bizFeeItemMapper.selectPage(page, Wrappers.<BizFeeItem>lambdaQuery()
                .eq(itemType != null, BizFeeItem::getItemType, itemType)
                .eq(status != null, BizFeeItem::getStatus, status)
                .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                        .like(BizFeeItem::getItemCode, query.getKeyword())
                        .or().like(BizFeeItem::getItemName, query.getKeyword()))
                .orderByAsc(BizFeeItem::getItemType)
                .orderByAsc(BizFeeItem::getItemCode)));
    }

    /**
     * 启用中的收费项目列表，供收费界面选择。
     */
    public List<BizFeeItem> listFeeItems(Integer itemType) {
        return bizFeeItemMapper.selectList(Wrappers.<BizFeeItem>lambdaQuery()
                .eq(itemType != null, BizFeeItem::getItemType, itemType)
                .eq(BizFeeItem::getStatus, 1)
                .orderByAsc(BizFeeItem::getItemType)
                .orderByAsc(BizFeeItem::getItemCode));
    }

    /**
     * 新增或修改收费项目。
     */
    public BizFeeItem saveFeeItem(BizFeeItem item) {
        if (item.getItemCode() == null || item.getItemCode().isBlank()) {
            throw BizException.badRequest("项目编码不能为空");
        }
        if (item.getItemName() == null || item.getItemName().isBlank()) {
            throw BizException.badRequest("项目名称不能为空");
        }
        if (item.getPrice() == null) {
            item.setPrice(BigDecimal.ZERO);
        }
        if (item.getStatus() == null) {
            item.setStatus(1);
        }
        if (item.getId() == null) {
            long exists = bizFeeItemMapper.selectCount(Wrappers.<BizFeeItem>lambdaQuery()
                    .eq(BizFeeItem::getItemCode, item.getItemCode()));
            if (exists > 0) {
                throw BizException.badRequest("项目编码已存在");
            }
            bizFeeItemMapper.insert(item);
        } else {
            bizFeeItemMapper.updateById(item);
        }
        return item;
    }

    private LocalDate parseDay(String text, LocalDate defaultValue) {
        if (StrUtil.isBlank(text)) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(text, DAY);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String payTypeName(Integer payType) {
        if (payType == null) {
            return "未知";
        }
        return switch (payType) {
            case 1 -> "现金";
            case 2 -> "微信";
            case 3 -> "支付宝";
            case 4 -> "医保";
            case 5 -> "银行卡";
            default -> "其他";
        };
    }

    private String toStr(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
