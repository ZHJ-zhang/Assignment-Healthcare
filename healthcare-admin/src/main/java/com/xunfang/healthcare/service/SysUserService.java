package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.dto.PasswordRequest;
import com.xunfang.healthcare.entity.SysUser;
import com.xunfang.healthcare.entity.SysUserRole;
import com.xunfang.healthcare.mapper.SysUserMapper;
import com.xunfang.healthcare.mapper.SysUserRoleMapper;
import com.xunfang.healthcare.security.SecurityUtils;
import com.xunfang.healthcare.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理。
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户分页查询。
     */
    public PageResult<SysUser> page(PageQuery query, Long deptId, Integer status) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        PageResult<SysUser> result = PageResult.of(sysUserMapper.selectPage(page, Wrappers.<SysUser>lambdaQuery()
                .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                        .like(SysUser::getUsername, query.getKeyword()).or()
                        .like(SysUser::getRealName, query.getKeyword()).or()
                        .eq(SysUser::getPhoneHash, CryptoUtil.digest(query.getKeyword())))
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .eq(status != null, SysUser::getStatus, status)
                .ge(StrUtil.isNotBlank(query.getStartTime()),
                        SysUser::getCreateTime, query.getStartTime())
                .le(StrUtil.isNotBlank(query.getEndTime()),
                        SysUser::getCreateTime, query.getEndTime() + " 23:59:59")
                .orderByDesc(SysUser::getCreateTime)));
        result.getList().forEach(this::decryptPhone);
        return result;
    }

    /**
     * 用户详情。
     */
    public SysUser detail(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw BizException.badRequest("用户不存在");
        }
        decryptPhone(user);
        return user;
    }

    /**
     * 查询用户拥有的角色 ID。
     */
    public List<Long> listRoleIds(Long userId) {
        return sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }

    /**
     * 新增用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUser create(SysUser user, List<Long> roleIds, String rawPassword) {
        if (existsByUsername(user.getUsername(), null)) {
            throw BizException.badRequest("登录账号已存在");
        }
        String password = rawPassword == null || rawPassword.isBlank() ? "123456" : rawPassword;
        user.setId(null);
        user.setPassword(passwordEncoder.encode(password));
        encryptPhone(user);
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        sysUserMapper.insert(user);
        saveUserRoles(user.getId(), roleIds);
        return user;
    }

    /**
     * 修改用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUser update(SysUser user, List<Long> roleIds) {
        if (user.getId() == null) {
            throw BizException.badRequest("用户 ID 不能为空");
        }
        SysUser current = detail(user.getId());
        if (existsByUsername(current.getUsername(), user.getId())) {
            throw BizException.badRequest("登录账号已存在");
        }
        user.setPassword(null);
        user.setUsername(null);
        user.setLastLoginTime(null);
        if (StrUtil.isNotBlank(user.getPhone())) {
            encryptPhone(user);
        }
        sysUserMapper.updateById(user);
        if (roleIds != null) {
            sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, user.getId()));
            saveUserRoles(user.getId(), roleIds);
        }
        return user;
    }

    /**
     * 删除用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        detail(id);
        if (id.equals(SecurityUtils.getUserIdOrNull())) {
            throw BizException.badRequest("不能删除当前登录用户");
        }
        sysUserMapper.deleteById(id);
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
    }

    /**
     * 启用 / 禁用用户。
     */
    public void updateStatus(Long id, Integer status) {
        detail(id);
        if (id.equals(SecurityUtils.getUserIdOrNull())) {
            throw BizException.badRequest("不能修改当前登录用户的状态");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        sysUserMapper.updateById(update);
    }

    /**
     * 重置密码。
     */
    public void resetPassword(Long id, String newPassword) {
        detail(id);
        String password = newPassword == null || newPassword.isBlank() ? "123456" : newPassword;
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(password));
        sysUserMapper.updateById(update);
    }

    /**
     * 修改自己的密码。
     */
    public void changePassword(PasswordRequest request) {
        SysUser current = detail(SecurityUtils.getUserId());
        if (!passwordEncoder.matches(request.getOldPassword(), current.getPassword())) {
            throw BizException.badRequest("原密码不正确");
        }
        SysUser update = new SysUser();
        update.setId(current.getId());
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        update.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(update);
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            sysUserRoleMapper.insert(relation);
        }
    }

    private boolean existsByUsername(String username, Long excludeId) {
        return sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId)) > 0;
    }

    private void encryptPhone(SysUser user) {
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            return;
        }
        user.setPhoneHash(CryptoUtil.digest(user.getPhone()));
        user.setPhone(CryptoUtil.encrypt(user.getPhone()));
    }

    private void decryptPhone(SysUser user) {
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            user.setPhone(CryptoUtil.decrypt(user.getPhone()));
        }
    }
}
