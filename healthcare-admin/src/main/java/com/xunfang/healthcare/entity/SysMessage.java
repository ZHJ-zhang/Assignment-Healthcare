package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内消息表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 接收人 ID，为空表示全体接收。
     */
    private Long receiverId;

    private String title;
    private String content;

    /**
     * 消息类型 1系统2待办3预警。
     */
    private Integer msgType;

    /**
     * 是否已读 0未读1已读。
     */
    private Integer isRead;

    /**
     * 来源业务单号。
     */
    private String bizId;
}
