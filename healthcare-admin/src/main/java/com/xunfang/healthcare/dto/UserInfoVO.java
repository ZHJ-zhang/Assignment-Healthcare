package com.xunfang.healthcare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 当前登录用户信息。
 */
@Data
@Schema(description = "当前登录用户信息")
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private Integer gender;
    private Long deptId;
    private String deptName;

    /**
     * 数据范围：1 全部、2 本部门及以下、3 本部门、4 仅本人。
     * <p>前端靠它决定科室下拉是否锁死、列表是否需要提示"只看自己名下的患者"。</p>
     */
    private Integer dataScope;

    /**
     * 角色编码集合。
     */
    private Set<String> roles;

    /**
     * 权限标识集合。
     */
    private Set<String> permissions;
}
