package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 安全配置表，对应需求文档模块十二的访问控制配置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_security_config")
public class SysSecurityConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 登录失败锁定次数。
     */
    private Integer loginFailLimit;

    /**
     * 锁定时长（分钟）。
     */
    private Integer lockMinutes;

    /**
     * 会话超时时间（分钟）。
     */
    private Integer sessionTimeout;

    /**
     * IP 白名单，多个用逗号分隔。
     */
    private String ipWhitelist;

    /**
     * 是否开启双因素认证。
     */
    private Integer twoFactorEnabled;

    /**
     * 是否开启敏感数据脱敏。
     */
    private Integer maskEnabled;

    /**
     * 密码最小长度。
     */
    private Integer passwordMinLength;

    /**
     * 密码有效期（天），0 表示永不过期。
     */
    private Integer passwordExpireDays;
}
