package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 医嘱表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_medical_order")
public class BizMedicalOrder extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 医嘱号。
     */
    private String orderNo;

    private Long patientId;
    private Long admissionId;
    private Long deptId;

    /**
     * 医生 ID。
     */
    private Long doctorId;

    /**
     * 医嘱类型 1长期2临时。
     */
    private Integer orderType;

    /**
     * 医嘱内容。
     */
    private String content;

    /**
     * 剂量。
     */
    private String dosage;

    /**
     * 执行频次。
     */
    private String frequency;

    /**
     * 开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 结束时间。
     */
    private LocalDateTime endTime;

    /**
     * 状态 0未执行1执行中2已完成3已停嘱。
     */
    private Integer status;
}
