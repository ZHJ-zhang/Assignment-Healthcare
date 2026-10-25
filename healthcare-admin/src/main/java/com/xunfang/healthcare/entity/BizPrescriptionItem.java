package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 处方明细表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_prescription_item")
public class BizPrescriptionItem extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long prescriptionId;
    private Long drugId;

    /**
     * 药品名称快照。
     */
    private String drugName;

    /**
     * 规格。
     */
    private String spec;

    /**
     * 单位。
     */
    private String unit;

    /**
     * 数量。
     */
    private Integer quantity;

    /**
     * 单价。
     */
    private BigDecimal price;

    /**
     * 小计。
     */
    private BigDecimal amount;

    /**
     * 用法。
     * <p>usage 在 MySQL 中是关键字，必须用反引号包裹，否则 MyBatis-Plus 生成的
     * INSERT/SELECT 会因列名未转义而报 SQL 语法错误。</p>
     */
    @TableField("`usage`")
    private String usage;

    /**
     * 频次。
     */
    private String frequency;
}
