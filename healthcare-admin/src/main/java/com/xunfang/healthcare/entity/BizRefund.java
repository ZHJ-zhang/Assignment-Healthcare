package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退费单表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_refund")
public class BizRefund extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 退费单号。
     */
    private String refundNo;

    private Long chargeId;
    private Long patientId;

    /**
     * 退费金额。
     */
    private BigDecimal refundAmount;

    /**
     * 退费方式 1现金2原路退回。
     */
    private Integer refundType;

    /**
     * 状态 0待审核1已审核2已退费3已驳回。
     */
    private Integer status;

    /**
     * 审核人。
     */
    private Long auditorId;

    /**
     * 审核时间。
     */
    private LocalDateTime auditTime;

    /**
     * 退费时间。
     */
    private LocalDateTime refundTime;

    /**
     * 原因。
     */
    private String reason;
}
