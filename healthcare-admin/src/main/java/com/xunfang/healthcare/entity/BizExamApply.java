package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检查检验申请表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_exam_apply")
public class BizExamApply extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 申请单号。
     */
    private String applyNo;

    private Long patientId;
    private Long doctorId;
    private Long deptId;

    /**
     * 就诊ID，指向 biz_registration.id。
     */
    private Long visitId;

    /**
     * 类型 1检验2检查（影像）。
     */
    private Integer examType;

    /**
     * 项目名称。
     */
    private String itemName;

    /**
     * 临床诊断。
     */
    private String clinicalDiagnosis;

    /**
     * 检查费金额，收费台按此金额生成收费明细。
     */
    private BigDecimal amount;

    /**
     * 申请时间。
     */
    private LocalDateTime applyTime;

    /**
     * 状态 0待缴费1待采样2检验中3已出报告。
     */
    private Integer status;

    /**
     * 审核人。
     */
    private Long auditorId;

    /**
     * 审核时间。
     */
    private LocalDateTime auditTime;
}
