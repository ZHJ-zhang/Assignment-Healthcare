package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 住院登记表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_admission")
public class BizAdmission extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 住院号。
     */
    private String admissionNo;

    private Long patientId;
    private Long deptId;
    private Long bedId;

    /**
     * 主治医生。
     */
    private Long doctorId;

    /**
     * 入院日期。
     */
    private LocalDate admissionDate;

    /**
     * 出院日期。
     */
    private LocalDate dischargeDate;

    /**
     * 押金金额。
     */
    private BigDecimal depositAmount;

    /**
     * 医保类型 1自费2城镇职工3城乡居民4异地。
     */
    private Integer insuranceType;

    /**
     * 医保号。
     */
    private String insuranceNo;

    /**
     * 诊断。
     */
    private String diagnosis;

    /**
     * 状态 0在院1已出院2已结算。
     */
    private Integer status;
}
