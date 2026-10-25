package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 病程记录表，对应入院记录、病程记录与出院小结。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_course_record")
public class BizCourseRecord extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long patientId;
    private Long admissionId;
    private Long deptId;

    /**
     * 记录类型 1入院记录2病程记录3出院小结4手术记录。
     */
    private Integer recordType;

    /**
     * 标题。
     */
    private String title;

    /**
     * 记录内容。
     */
    private String content;

    /**
     * 记录时间。
     */
    private LocalDateTime recordTime;

    /**
     * 记录医生。
     */
    private Long doctorId;
}
