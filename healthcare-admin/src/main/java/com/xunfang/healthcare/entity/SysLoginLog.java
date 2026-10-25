package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志表。
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String ip;
    private String location;
    private String browser;
    private String os;

    /**
     * 状态 0失败1成功。
     */
    private Integer status;

    /**
     * 提示信息。
     */
    private String msg;

    private LocalDateTime loginTime;
}
