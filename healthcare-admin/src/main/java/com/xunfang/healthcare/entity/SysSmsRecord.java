package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短信发送记录表。
 */
@Data
@TableName("sys_sms_record")
public class SysSmsRecord {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String phone;

    /**
     * 模板编码。
     */
    private String templateCode;

    private String content;

    /**
     * 发送状态 0发送中1成功2失败。
     */
    private Integer sendStatus;

    /**
     * 失败原因。
     */
    private String errorMsg;

    private LocalDateTime sendTime;
}
