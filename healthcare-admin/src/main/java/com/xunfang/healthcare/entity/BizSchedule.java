package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 医生排班表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_schedule")
public class BizSchedule extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long doctorId;
    private Long deptId;

    /**
     * 排班日期。
     */
    private LocalDate scheduleDate;

    /**
     * 时段 1上午2下午3晚上。
     */
    private Integer timePeriod;

    /**
     * 号别 1普通2专家3急诊。
     */
    private Integer regType;

    /**
     * 总号源。
     */
    private Integer totalNum;

    /**
     * 剩余号源。
     */
    private Integer remainNum;

    /**
     * 挂号费。
     */
    private BigDecimal regFee;

    /**
     * 状态 0停诊1正常。
     */
    private Integer status;
}
