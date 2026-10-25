package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 床位表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_bed")
public class BizBed extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 所属病区。
     */
    private Long wardId;

    /**
     * 病区名称。
     */
    private String wardName;

    /**
     * 科室 ID。
     */
    private Long deptId;

    /**
     * 床位号。
     */
    private String bedNo;

    /**
     * 床位类型 1普通2VIP3ICU4观察。
     */
    private Integer bedType;

    /**
     * 床位费（元/天）。
     */
    private BigDecimal price;

    /**
     * 状态 0空闲1占用2预留3维护。
     */
    private Integer status;

    /**
     * 当前患者 ID。
     */
    private Long patientId;
}
