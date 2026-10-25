package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收费单表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_charge")
public class BizCharge extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 收费单号。
     */
    private String chargeNo;

    private Long patientId;
    private Long deptId;

    /**
     * 就诊ID，指向 biz_registration.id。
     */
    private Long visitId;

    /**
     * 关联处方 ID。
     */
    private Long prescriptionId;

    /**
     * 收费类型 1挂号费2门诊费3住院费（对应接口文档的 charge_type）。
     */
    private Integer visitType;

    /**
     * 总金额。
     */
    private BigDecimal totalAmount;

    /**
     * 优惠金额。
     */
    private BigDecimal discountAmount;

    /**
     * 实收金额。
     */
    private BigDecimal actualAmount;

    /**
     * 支付方式 1现金2银行卡3微信4支付宝5医保。
     */
    private Integer payType;

    /**
     * 支付状态 0待支付1已支付2已退费。
     */
    private Integer payStatus;

    /**
     * 收费员。
     */
    private Long operatorId;

    /**
     * 收费时间。
     */
    private LocalDateTime chargeTime;
}
