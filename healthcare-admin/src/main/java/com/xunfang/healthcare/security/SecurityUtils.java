package com.xunfang.healthcare.security;

import com.xunfang.healthcare.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * 当前登录用户工具类。
 */
public final class SecurityUtils {

    public static final String ROLE_ADMIN = "admin";

    private SecurityUtils() {
    }

    public static Optional<LoginUser> getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return Optional.empty();
        }
        return Optional.of(loginUser);
    }

    public static LoginUser getLoginUserRequired() {
        return getLoginUser().orElseThrow(() -> new BizException(401, "未登录"));
    }

    public static Long getUserIdOrNull() {
        return getLoginUser().map(LoginUser::getUserId).orElse(null);
    }

    public static Long getUserId() {
        return getLoginUserRequired().getUserId();
    }

    public static String getUsername() {
        return getLoginUserRequired().getUsername();
    }

    public static Long getDeptId() {
        return getLoginUser().map(LoginUser::getDeptId).orElse(null);
    }

    public static Set<String> getPermissions() {
        return getLoginUser().map(LoginUser::getPermissions).orElse(Collections.emptySet());
    }

    public static Set<String> getRoles() {
        return getLoginUser().map(LoginUser::getRoles).orElse(Collections.emptySet());
    }

    public static boolean isAdmin() {
        return getRoles().contains(ROLE_ADMIN);
    }
}
