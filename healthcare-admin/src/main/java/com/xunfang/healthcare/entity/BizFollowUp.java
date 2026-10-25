package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 随访表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_follow_up")
public class BizFollowUp extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long patientId;
    private Long deptId;

    /**
     * 计划随访日期。
     */
    private LocalDate planDate;

    /**
     * 随访方式 1电话2上门3短信。
     */
    private Integer followType;

    /**
     * 随访内容。
     */
    private String content;

    /**
     * 随访结果。
     */
    private String result;

    /**
     * 随访人。
     */
    private Long operatorId;

    /**
     * 状态 0待随访1已完成。
     */
    private Integer status;
}
