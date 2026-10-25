package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 危急值记录表，跟踪危急值的通知与处理闭环。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_critical_value")
public class BizCriticalValue extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long reportId;
    private Long patientId;
    private Long deptId;

    /**
     * 指标名称。
     */
    private String itemName;

    /**
     * 检验值。
     */
    private String value;

    /**
     * 参考范围。
     */
    private String referenceRange;

    /**
     * 状态 0待通知1已通知2已处理。
     */
    private Integer status;

    /**
     * 通知时间。
     */
    private LocalDateTime notifyTime;

    /**
     * 处理人。
     */
    private Long handlerId;

    /**
     * 处理时间。
     */
    private LocalDateTime handleTime;

    /**
     * 处理说明。
     */
    private String handleRemark;
}
