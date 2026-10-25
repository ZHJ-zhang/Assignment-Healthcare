package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 处方表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_prescription")
public class BizPrescription extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 处方号。
     */
    private String prescNo;

    private Long patientId;
    private Long doctorId;
    private Long deptId;

    /**
     * 就诊ID，指向 biz_registration.id。
     */
    private Long visitId;

    /**
     * 处方类型 1西药2中药。
     */
    private Integer prescType;

    /**
     * 总金额。
     */
    private BigDecimal totalAmount;

    /**
     * 状态 0待收费1已审核(历史)2已收费3已发药4已退5已作废。
     * <p>
     * 审核环节已废弃（处方由医生开立即生效），1 只用于兼容历史数据，语义等同 0。
     */
    private Integer status;

    /**
     * 审核人（历史字段，审核环节下线后不再写入）。
     */
    private Long auditorId;

    /**
     * 审核时间。
     */
    private LocalDateTime auditTime;

    /**
     * 备注。
     */
    private String remark;
}
