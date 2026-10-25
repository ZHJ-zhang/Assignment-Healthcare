package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 患者档案表。
 *
 * <p>身份证号与手机号按模块十二要求加密存储，
 * 同时保留摘要列用于精确检索。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_patient")
public class BizPatient extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 患者编号。
     */
    private String patientNo;

    /**
     * 姓名。
     */
    private String name;

    /**
     * 性别 0女1男。
     */
    private Integer gender;

    /**
     * 出生日期。
     */
    private LocalDate birthDate;

    /**
     * 身份证号（密文）。
     */
    private String idCard;

    /**
     * 身份证号摘要，用于精确检索。
     */
    private String idCardHash;

    /**
     * 手机号（密文）。
     */
    private String phone;

    /**
     * 手机号摘要，用于精确检索。
     */
    private String phoneHash;

    /**
     * 住址。
     */
    private String address;

    /**
     * 血型。
     */
    private String bloodType;

    /**
     * 过敏史。
     */
    private String allergyHistory;

    /**
     * 既往病史。
     */
    private String medicalHistory;

    /**
     * 家族病史。
     */
    private String familyHistory;

    /**
     * 当前就诊卡号。
     */
    private String cardNo;

    /**
     * 就诊卡余额。
     */
    private java.math.BigDecimal cardBalance;

    /**
     * 患者标签，逗号分隔。
     */
    private String tags;

    /**
     * 主要接诊科室。
     */
    private Long deptId;

    /**
     * 状态 0归档1在档。
     */
    private Integer status;
}
