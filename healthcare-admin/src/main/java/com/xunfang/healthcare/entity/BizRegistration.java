package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 挂号记录表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_registration")
public class BizRegistration extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 挂号单号。
     */
    private String regNo;

    private Long patientId;
    private Long doctorId;
    private Long deptId;
    private Long scheduleId;

    /**
     * 挂号类型。
     */
    private Integer regType;

    /**
     * 挂号费。
     */
    private BigDecimal regFee;

    /**
     * 排队号。
     */
    private Integer queueNo;

    /**
     * 状态 0待就诊1就诊中2已完成3已退号。
     */
    private Integer status;

    /**
     * 挂号时间。
     */
    private LocalDateTime regTime;

    /**
     * 退号原因。
     */
    private String cancelReason;
}
