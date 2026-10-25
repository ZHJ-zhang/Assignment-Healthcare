package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 医嘱执行记录表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_order_execution")
public class BizOrderExecution extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long patientId;

    /**
     * 执行时间。
     */
    private LocalDateTime executeTime;

    /**
     * 执行人（护士）。
     */
    private Long executorId;

    /**
     * 执行结果描述。
     */
    private String result;

    /**
     * 状态 1已完成2异常。
     */
    private Integer status;
}
