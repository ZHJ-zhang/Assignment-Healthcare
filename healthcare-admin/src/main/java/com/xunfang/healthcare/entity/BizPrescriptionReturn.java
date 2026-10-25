package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 处方退药明细表。
 * <p>记录每一次退药退了哪条处方明细，用于判断「哪些药已经退过」
 * 以及「是否整张处方已全部退回」——只有全部明细都退完，处方才进入终态已退。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_prescription_return")
public class BizPrescriptionReturn extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long prescriptionId;

    /**
     * 被退回的处方明细 ID。
     */
    private Long itemId;

    private Long drugId;

    /**
     * 退回数量。
     */
    private Integer quantity;

    /**
     * 本次退回金额（等于明细小计）。
     */
    private BigDecimal amount;

    /**
     * 关联的退费单 ID，用于把实物退回和退款对账串起来。
     */
    private Long refundId;

    private Long warehouseId;

    /**
     * 退药原因。
     */
    private String reason;
}
