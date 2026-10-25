package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 预约挂号表，支持线上预约、取消与改约。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_appointment")
public class BizAppointment extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long patientId;
    private Long doctorId;
    private Long deptId;
    private Long scheduleId;

    /**
     * 转挂号后的挂号记录ID。取号（预约转挂号）后回写，退号时据此回退预约状态。
     */
    private Long registrationId;

    /**
     * 预约日期。
     */
    private LocalDate appointmentDate;

    /**
     * 时段 1上午2下午3晚上。
     */
    private Integer timePeriod;

    /**
     * 号别 1普通2专家3急诊。
     */
    private Integer regType;

    /**
     * 来源 1线上2线下。
     */
    private Integer source;

    /**
     * 状态 0待就诊1已就诊2已取消3已改约。
     */
    private Integer status;

    /**
     * 取消原因。
     */
    private String cancelReason;
}
