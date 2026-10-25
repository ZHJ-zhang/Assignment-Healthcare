package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 居民健康档案表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_health_record")
public class BizHealthRecord extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 档案编号。
     */
    private String recordNo;

    /**
     * 居民姓名。
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
     * 手机号。
     */
    private String phone;

    /**
     * 所属社区。
     */
    private String community;

    /**
     * 档案类型 1健康档案2慢病管理。
     */
    private Integer recordType;

    /**
     * 慢病类型。
     */
    private String chronicType;

    /**
     * 既往史。
     */
    private String medicalHistory;

    /**
     * 家族史。
     */
    private String familyHistory;

    /**
     * 建档日期。
     */
    private LocalDate createDate;
}
