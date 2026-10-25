package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 检验检查报告表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_exam_report")
public class BizExamReport extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long applyId;
    private Long patientId;

    /**
     * 报告标题。
     */
    private String reportTitle;

    /**
     * 报告内容（结构化 JSON 或文本）。
     */
    private String reportContent;

    /**
     * 结论。
     */
    private String conclusion;

    /**
     * 是否危急值 0否1是。
     */
    private Integer criticalFlag;

    /**
     * 附件地址（影像文件等）。
     */
    private String attachment;

    /**
     * 审核人。
     */
    private Long auditorId;

    /**
     * 审核时间。
     */
    private LocalDateTime auditTime;

    /**
     * 报告时间。
     */
    private LocalDateTime reportTime;

    /**
     * 状态 0待审核1已发布。
     */
    private Integer status;
}
