package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 疫苗接种记录表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_vaccination")
public class BizVaccination extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 接种编号。
     */
    private String vaccinationNo;

    private Long patientId;

    /**
     * 姓名。
     */
    private String name;

    /**
     * 疫苗名称。
     */
    private String vaccineName;

    /**
     * 疫苗批号。
     */
    private String batchNo;

    /**
     * 接种剂次。
     */
    private Integer doseNo;

    /**
     * 接种日期。
     */
    private LocalDate vaccinationDate;

    /**
     * 接种部位。
     */
    private String injectionSite;

    /**
     * 接种医生。
     */
    private Long doctorId;

    /**
     * 下次接种日期。
     */
    private LocalDate nextDate;

    /**
     * 状态 0待接种1已接种2已逾期。
     */
    private Integer status;
}
