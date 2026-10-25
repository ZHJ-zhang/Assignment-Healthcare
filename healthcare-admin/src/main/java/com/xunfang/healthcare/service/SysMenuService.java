package com.xunfang.healthcare.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xunfang.healthcare.common.BizException;
import com.xunfang.healthcare.entity.SysMenu;
import com.xunfang.healthcare.entity.SysRoleMenu;
import com.xunfang.healthcare.mapper.SysMenuMapper;
import com.xunfang.healthcare.mapper.SysRoleMenuMapper;
import com.xunfang.healthcare.vo.MenuTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单与按钮权限管理。
 */
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /**
     * 菜单树（含按钮）。
     */
    public List<MenuTreeVO> tree() {
        List<SysMenu> all = sysMenuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getSort));
        return buildTree(all, 0L);
    }

    /**
     * 菜单详情。
     */
    public SysMenu detail(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw BizException.badRequest("菜单不存在");
        }
        return menu;
    }

    /**
     * 新增菜单。
     */
    public SysMenu create(SysMenu menu) {
        menu.setId(null);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        sysMenuMapper.insert(menu);
        return menu;
    }

    /**
     * 修改菜单。
     */
    public SysMenu update(SysMenu menu) {
        detail(menu.getId());
        if (menu.getId().equals(menu.getParentId())) {
            throw BizException.badRequest("上级菜单不能是自己");
        }
        sysMenuMapper.updateById(menu);
        return menu;
    }

    /**
     * 删除菜单。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        detail(id);
        if (sysMenuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, id)) > 0) {
            throw BizException.badRequest("存在子菜单，无法删除");
        }
        if (sysRoleMenuMapper.selectCount(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getMenuId, id)) > 0) {
            throw BizException.badRequest("菜单已被角色引用，无法删除");
        }
        sysMenuMapper.deleteById(id);
    }

    private List<MenuTreeVO> buildTree(List<SysMenu> all, Long parentId) {
        Map<Long, List<SysMenu>> grouped = all.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId()));
        List<SysMenu> children = grouped.getOrDefault(parentId, new ArrayList<>());
        children.sort(Comparator.comparing((SysMenu menu) -> menu.getSort() == null ? 0 : menu.getSort()));
        List<MenuTreeVO> nodes = new ArrayList<>();
        for (SysMenu menu : children) {
            MenuTreeVO node = toNode(menu);
            node.setChildren(buildTree(all, menu.getId()));
            nodes.add(node);
        }
        return nodes;
    }

    private MenuTreeVO toNode(SysMenu menu) {
        MenuTreeVO node = new MenuTreeVO();
        node.setId(menu.getId());
        node.setParentId(menu.getParentId());
        node.setMenuName(menu.getMenuName());
        node.setMenuType(menu.getMenuType());
        node.setPath(menu.getPath());
        node.setComponent(menu.getComponent());
        node.setPerms(menu.getPerms());
        node.setIcon(menu.getIcon());
        node.setSort(menu.getSort());
        node.setVisible(menu.getVisible());
        node.setStatus(menu.getStatus());
        return node;
    }
}
