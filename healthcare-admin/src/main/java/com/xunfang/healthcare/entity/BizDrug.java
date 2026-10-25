package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 药品目录表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_drug")
public class BizDrug extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 药品编码。
     */
    private String drugCode;

    /**
     * 药品名称。
     */
    private String drugName;

    /**
     * 通用名。
     */
    private String genericName;

    /**
     * 规格。
     */
    private String spec;

    /**
     * 剂型。
     */
    private String dosageForm;

    /**
     * 单位。
     */
    private String unit;

    /**
     * 生产厂家。
     */
    private String manufacturer;

    /**
     * 批准文号。
     */
    private String approvalNo;

    /**
     * 进价。
     */
    private BigDecimal purchasePrice;

    /**
     * 售价。
     */
    private BigDecimal salePrice;

    /**
     * 分类。
     */
    private String category;

    /**
     * 是否特殊药品（麻精毒）。
     */
    private Integer isSpecial;

    /**
     * 状态 0停用1启用。
     */
    private Integer status;
}
