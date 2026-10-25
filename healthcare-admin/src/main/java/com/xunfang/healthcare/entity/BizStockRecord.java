package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 药品出入库流水表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_stock_record")
public class BizStockRecord extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long drugId;

    /**
     * 类型 1入库2出库3盘点4退药。
     */
    private Integer recordType;

    /**
     * 数量。
     */
    private Integer quantity;

    /**
     * 批号。
     */
    private String batchNo;

    /**
     * 库房 ID。
     */
    private Long warehouseId;

    /**
     * 操作人。
     */
    private Long operatorId;

    /**
     * 关联单据号。
     */
    private String bizNo;

    /**
     * 药品名称，列表查询时按药品 ID 冗余填充，不落库。
     */
    @TableField(exist = false)
    private String drugName;
}
