package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 体检记录表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_physical_exam")
public class BizPhysicalExam extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 体检编号。
     */
    private String examNo;

    private Long patientId;

    /**
     * 姓名。
     */
    private String name;

    /**
     * 体检套餐。
     */
    private String packageName;

    /**
     * 体检日期。
     */
    private LocalDate examDate;

    /**
     * 体检结果（JSON）。
     */
    private String result;

    /**
     * 健康评估。
     */
    private String assessment;

    /**
     * 总分。
     */
    private Integer score;

    /**
     * 状态 0待体检1已完成2已出报告。
     */
    private Integer status;
}
