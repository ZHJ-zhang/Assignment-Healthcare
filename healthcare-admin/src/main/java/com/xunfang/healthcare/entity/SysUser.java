package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 登录账号。
     */
    private String username;

    /**
     * 密码（BCrypt 加密）。
     */
    @JsonIgnore
    private String password;

    /**
     * 真实姓名。
     */
    private String realName;

    /**
     * 性别 0女1男。
     */
    private Integer gender;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 手机号 SHA-256 摘要，用于精确检索。
     */
    private String phoneHash;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 所属科室 ID。
     */
    private Long deptId;

    /**
     * 头像地址。
     */
    private String avatar;

    /**
     * 状态 0禁用1启用。
     */
    private Integer status;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 最后登录时间。
     */
    private LocalDateTime lastLoginTime;
}
