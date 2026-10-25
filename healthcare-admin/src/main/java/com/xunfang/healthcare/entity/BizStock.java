package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 库存表（按批号管理）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_stock")
public class BizStock extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long drugId;

    /**
     * 批号。
     */
    private String batchNo;

    /**
     * 库存数量。
     */
    private Integer quantity;

    /**
     * 预警数量。
     */
    private Integer warnNum;

    /**
     * 生产日期。
     */
    private LocalDate produceDate;

    /**
     * 有效期。
     */
    private LocalDate expireDate;

    /**
     * 库房 ID。
     */
    private Long warehouseId;

    /**
     * 锁定库存（已占用未发药）。
     */
    private Integer lockQuantity;

    /**
     * 药品名称，列表查询时按药品 ID 冗余填充，不落库。
     */
    @TableField(exist = false)
    private String drugName;
}
