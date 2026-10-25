package com.xunfang.healthcare.security;

import org.springframework.stereotype.Component;

/**
 * 方法级权限校验器，注册为名为 ss 的 Bean，供 {@code @PreAuthorize("@ss.hasPermi('...')")} 使用。
 */
@Component("ss")
public class PermissionChecker {

    /**
     * 是否拥有指定按钮权限，超级管理员恒为 true。
     */
    public boolean hasPermi(String permission) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        return permission != null && SecurityUtils.getPermissions().contains(permission);
    }

    /**
     * 是否拥有任意一个权限。
     */
    public boolean hasAnyPermi(String... permissions) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        if (permissions == null) {
            return false;
        }
        for (String permission : permissions) {
            if (hasPermi(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否拥有指定角色。
     */
    public boolean hasRole(String role) {
        return SecurityUtils.getRoles().contains(role);
    }
}
