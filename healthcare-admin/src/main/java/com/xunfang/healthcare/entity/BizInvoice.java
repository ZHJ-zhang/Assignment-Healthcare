package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发票表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_invoice")
public class BizInvoice extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 发票号码。
     */
    private String invoiceNo;

    /**
     * 发票代码。
     */
    private String invoiceCode;

    private Long chargeId;
    private Long patientId;

    /**
     * 抬头名称。
     */
    private String title;

    /**
     * 抬头税号。
     */
    private String taxNo;

    /**
     * 开票金额。
     */
    private BigDecimal amount;

    /**
     * 发票类型 1电子2纸质。
     */
    private Integer invoiceType;

    /**
     * 状态 0未开票1已开票2已作废。
     */
    private Integer status;

    /**
     * 开票时间。
     */
    private LocalDateTime invoiceTime;

    /**
     * 作废原因。
     */
    private String voidReason;
}
