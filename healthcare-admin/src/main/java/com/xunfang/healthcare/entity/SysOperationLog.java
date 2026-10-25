package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;

    /**
     * 所属模块。
     */
    private String module;

    /**
     * 操作描述。
     */
    private String operation;

    /**
     * 请求方法。
     */
    private String method;

    /**
     * 请求参数。
     */
    private String params;

    /**
     * IP 地址。
     */
    private String ip;

    /**
     * 浏览器 UA。
     */
    private String userAgent;

    /**
     * 状态 0失败1成功。
     */
    private Integer status;

    /**
     * 耗时（毫秒）。
     */
    private Integer costTime;

    private LocalDateTime createTime;
}
