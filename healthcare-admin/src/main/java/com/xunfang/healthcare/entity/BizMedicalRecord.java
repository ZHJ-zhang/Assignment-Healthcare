package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 电子病历表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_medical_record")
public class BizMedicalRecord extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long patientId;
    private Long deptId;

    /**
     * 就诊ID，指向 biz_registration.id，标识这份病历属于哪一次就诊。
     */
    private Long visitId;

    /**
     * 就诊流水号。
     */
    private String visitNo;

    /**
     * 就诊类型 1门诊2住院。
     */
    private Integer visitType;

    /**
     * 主诉。
     */
    private String chiefComplaint;

    /**
     * 现病史。
     */
    private String presentIllness;

    /**
     * 体格检查。
     */
    private String physicalExam;

    /**
     * 诊断。
     */
    private String diagnosis;

    /**
     * ICD 编码。
     */
    private String icdCode;

    /**
     * 医生 ID。
     */
    private Long doctorId;

    /**
     * 就诊时间。
     */
    private LocalDateTime visitTime;

    /**
     * 状态 0草稿1已提交2已归档。
     */
    private Integer status;
}
