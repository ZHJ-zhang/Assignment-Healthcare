package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出院结算与医保结算表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_settlement")
public class BizSettlement extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 结算单号。
     */
    private String settlementNo;

    private Long admissionId;
    private Long patientId;
    private Long deptId;

    /**
     * 费用总额。
     */
    private BigDecimal totalAmount;

    /**
     * 医保报销金额。
     */
    private BigDecimal insuranceAmount;

    /**
     * 个人自付金额。
     */
    private BigDecimal selfAmount;

    /**
     * 押金抵扣金额。
     */
    private BigDecimal depositAmount;

    /**
     * 实际退款金额。
     */
    private BigDecimal refundAmount;

    /**
     * 结算状态 0待结算1已结算2已退费。
     */
    private Integer status;

    /**
     * 结算时间。
     */
    private LocalDateTime settleTime;

    /**
     * 退费时间。
     */
    private LocalDateTime refundTime;

    /**
     * 退费原因。
     */
    private String refundReason;

    /**
     * 医保结算单号（第三方返回）。
     */
    private String insuranceTradeNo;
}
