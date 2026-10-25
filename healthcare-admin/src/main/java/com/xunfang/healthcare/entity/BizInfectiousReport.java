package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 传染病上报表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_infectious_report")
public class BizInfectiousReport extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 上报单号。
     */
    private String reportNo;

    /**
     * 姓名。
     */
    private String name;

    /**
     * 性别 0女1男。
     */
    private Integer gender;

    private LocalDate birthDate;

    /**
     * 身份证号（密文）。
     */
    private String idCard;

    /**
     * 联系电话。
     */
    private String phone;

    /**
     * 住址。
     */
    private String address;

    /**
     * 病种名称。
     */
    private String diseaseName;

    /**
     * ICD 编码。
     */
    private String icdCode;

    /**
     * 发病日期。
     */
    private LocalDate onsetDate;

    /**
     * 报告日期。
     */
    private LocalDate reportDate;

    /**
     * 上报状态 0待上报1已上报2已审核。
     */
    private Integer status;

    /**
     * 审核时间。
     */
    private LocalDateTime auditTime;

    /**
     * 审核意见。
     */
    private String auditRemark;
}
