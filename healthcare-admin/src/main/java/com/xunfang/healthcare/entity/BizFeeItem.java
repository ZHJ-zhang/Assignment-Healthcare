package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 收费项目与价格表，对应模块八的价格管理。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_fee_item")
public class BizFeeItem extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 项目编码。
     */
    private String itemCode;

    /**
     * 项目名称。
     */
    private String itemName;

    /**
     * 项目分类：1诊疗2检查3化验4药品5材料6床位。
     */
    private Integer itemType;

    /**
     * 计价单位。
     */
    private String unit;

    /**
     * 单价。
     */
    private BigDecimal price;

    /**
     * 是否医保目录 0否1是。
     */
    private Integer medicalInsurance;

    /**
     * 状态 0停用1启用。
     */
    private Integer status;
}
