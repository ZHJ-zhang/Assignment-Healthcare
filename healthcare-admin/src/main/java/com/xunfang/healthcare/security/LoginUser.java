package com.xunfang.healthcare.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 登录用户上下文，同时承载按钮权限与数据权限所需的角色信息。
 */
@Data
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String realName;
    private String password;
    private Long deptId;
    private String deptName;
    private Integer status;

    /**
     * 角色编码集合，例如 admin / doctor。
     */
    private Set<String> roles = new HashSet<>();

    /**
     * 权限标识集合，例如 system:user:list。
     */
    private Set<String> permissions = new HashSet<>();

    /**
     * 是否拥有全部数据权限。
     */
    private boolean admin;

    /**
     * 数据范围：1 全部、2 本部门及以下、3 本部门、4 仅本人。多角色时取范围最宽的。
     */
    private Integer dataScope;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == null || status == 1;
    }
}
