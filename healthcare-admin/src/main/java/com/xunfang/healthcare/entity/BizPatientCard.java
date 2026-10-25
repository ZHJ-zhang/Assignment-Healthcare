package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 就诊卡表，支持发卡、挂失、补卡与余额记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_patient_card")
public class BizPatientCard extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long patientId;

    /**
     * 卡号。
     */
    private String cardNo;

    /**
     * 卡类型 1普通2职工3贵宾。
     */
    private Integer cardType;

    /**
     * 卡余额。
     */
    private BigDecimal balance;

    /**
     * 状态 0挂失1正常2注销（与数据库注释一致）。
     */
    private Integer status;

    /**
     * 发卡时间。
     */
    private LocalDateTime issueTime;

    /**
     * 挂失时间。
     */
    private LocalDateTime lossTime;

    /**
     * 备注。
     */
    private String remark;
}
