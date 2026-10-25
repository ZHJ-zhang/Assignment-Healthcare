package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 收费明细表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_charge_item")
public class BizChargeItem extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long chargeId;

    /**
     * 项目类型：1诊疗2检查3化验4药品5材料6床位。
     */
    private Integer itemType;

    /**
     * 项目 ID。
     */
    private Long itemId;

    /**
     * 项目名称快照。
     */
    private String itemName;

    /**
     * 数量。
     */
    private Integer quantity;

    /**
     * 单价。
     */
    private BigDecimal price;

    /**
     * 金额。
     */
    private BigDecimal amount;
}
