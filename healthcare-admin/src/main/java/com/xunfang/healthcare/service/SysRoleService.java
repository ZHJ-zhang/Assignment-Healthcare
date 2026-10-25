package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.entity.SysRole;
import com.xunfang.healthcare.entity.SysRoleMenu;
import com.xunfang.healthcare.mapper.SysRoleMapper;
import com.xunfang.healthcare.mapper.SysRoleMenuMapper;
import com.xunfang.healthcare.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色与权限分配。
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * 角色列表（下拉选择用）。
     */
    public List<SysRole> list() {
        return sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort));
    }

    /**
     * 角色分页。
     */
    public PageResult<SysRole> page(PageQuery query) {
        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(sysRoleMapper.selectPage(page, Wrappers.<SysRole>lambdaQuery()
                .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                        .like(SysRole::getRoleName, query.getKeyword()).or()
                        .like(SysRole::getRoleCode, query.getKeyword()))
                .orderByAsc(SysRole::getSort)));
    }

    /**
     * 角色详情。
     */
    public SysRole detail(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw BizException.badRequest("角色不存在");
        }
        return role;
    }

    /**
     * 新增角色。
     */
    public SysRole create(SysRole role) {
        if (existsByCode(role.getRoleCode(), null)) {
            throw BizException.badRequest("角色编码已存在");
        }
        role.setId(null);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        sysRoleMapper.insert(role);
        return role;
    }

    /**
     * 修改角色。
     */
    public SysRole update(SysRole role) {
        detail(role.getId());
        if (existsByCode(role.getRoleCode(), role.getId())) {
            throw BizException.badRequest("角色编码已存在");
        }
        sysRoleMapper.updateById(role);
        return role;
    }

    /**
     * 删除角色。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        detail(id);
        if (sysUserRoleMapper.selectCount(Wrappers.<com.xunfang.healthcare.entity.SysUserRole>lambdaQuery()
                .eq(com.xunfang.healthcare.entity.SysUserRole::getRoleId, id)) > 0) {
            throw BizException.badRequest("该角色下仍有用户，无法删除");
        }
        sysRoleMapper.deleteById(id);
        sysRoleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
    }

    /**
     * 查询角色拥有的菜单 ID。
     */
    public List<Long> listMenuIds(Long roleId) {
        return sysRoleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    /**
     * 分配菜单权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> menuIds) {
        detail(roleId);
        sysRoleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            sysRoleMenuMapper.insert(relation);
        }
    }

    private boolean existsByCode(String roleCode, Long excludeId) {
        return sysRoleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, roleCode)
                .ne(excludeId != null, SysRole::getId, excludeId)) > 0;
    }
}
