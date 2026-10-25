package com.xunfang.healthcare.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xunfang.healthcare.entity.SysMenu;
import com.xunfang.healthcare.entity.SysRole;
import com.xunfang.healthcare.entity.SysUser;
import com.xunfang.healthcare.mapper.SysMenuMapper;
import com.xunfang.healthcare.mapper.SysRoleMapper;
import com.xunfang.healthcare.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 根据登录账号装配角色与按钮权限。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public LoginUser loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username).last("limit 1"));
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在：" + username);
        }
        return buildLoginUser(user);
    }

    public LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setPassword(user.getPassword());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setStatus(user.getStatus());

        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(user.getId());
        Set<String> roleCodes = roles.stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .map(SysRole::getRoleCode)
                .collect(Collectors.toCollection(HashSet::new));
        loginUser.setRoles(roleCodes);
        loginUser.setAdmin(roleCodes.contains(SecurityUtils.ROLE_ADMIN));

        // 数据范围取多角色里最宽的一档（数值越小越宽），管理员直接给"全部"
        if (loginUser.isAdmin()) {
            loginUser.setDataScope(DataScopeHelper.SCOPE_ALL);
        } else {
            Integer widest = roles.stream()
                    .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                    .map(SysRole::getDataScope)
                    .filter(java.util.Objects::nonNull)
                    .min(Integer::compareTo)
                    .orElse(DataScopeHelper.SCOPE_ALL);
            loginUser.setDataScope(widest);
        }

        if (loginUser.isAdmin()) {
            loginUser.setPermissions(Set.of("*:*:*"));
        } else {
            List<SysMenu> menus = sysMenuMapper.selectMenusByUserId(user.getId());
            loginUser.setPermissions(menus.stream()
                    .map(SysMenu::getPerms)
                    .filter(perms -> perms != null && !perms.isBlank())
                    .collect(Collectors.toCollection(HashSet::new)));
        }
        return loginUser;
    }
}
