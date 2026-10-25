package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 叫号记录表，留痕候诊与叫号过程。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_call_record")
public class BizCallRecord extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private Long deptId;
    private Long registrationId;
    private Long patientId;

    /**
     * 排队号。
     */
    private Integer queueNo;

    /**
     * 叫号次序。
     */
    private Integer callNo;

    /**
     * 叫号时间。
     */
    private LocalDateTime callTime;

    /**
     * 状态 1已叫号2已就诊3已过号。
     */
    private Integer status;
}
