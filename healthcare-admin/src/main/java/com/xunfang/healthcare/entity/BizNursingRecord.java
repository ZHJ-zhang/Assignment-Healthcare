package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 护理记录表，含体温单与生命体征。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_nursing_record")
public class BizNursingRecord extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long patientId;
    private Long admissionId;
    private Long deptId;

    /**
     * 记录类型 1体温单2生命体征3护理记录。
     */
    private Integer recordType;

    /**
     * 体温（摄氏度）。
     */
    private BigDecimal temperature;

    /**
     * 收缩压。
     */
    private Integer systolicPressure;

    /**
     * 舒张压。
     */
    private Integer diastolicPressure;

    /**
     * 心率。
     */
    private Integer heartRate;

    /**
     * 呼吸次数。
     */
    private Integer respiration;

    /**
     * 血氧饱和度。
     */
    private BigDecimal bloodOxygen;

    /**
     * 记录内容。
     */
    private String content;

    /**
     * 记录时间。
     */
    private LocalDateTime recordTime;

    /**
     * 记录护士。
     */
    private Long nurseId;
}
