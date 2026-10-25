package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysMenu;
import com.xunfang.healthcare.service.SysMenuService;
import com.xunfang.healthcare.vo.MenuTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单与按钮权限管理。
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    /**
     * 菜单树。
     */
    @Operation(summary = "菜单树")
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/tree")
    public Result<List<MenuTreeVO>> tree() {
        return Result.success(sysMenuService.tree());
    }

    /**
     * 菜单详情。
     */
    @Operation(summary = "菜单详情")
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/{id}")
    public Result<SysMenu> detail(@PathVariable Long id) {
        return Result.success(sysMenuService.detail(id));
    }

    /**
     * 新增菜单。
     */
    @Operation(summary = "新增菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @OperationLog(module = "系统管理", operation = "新增菜单")
    @PostMapping
    public Result<SysMenu> create(@RequestBody SysMenu menu) {
        return Result.success(sysMenuService.create(menu));
    }

    /**
     * 修改菜单。
     */
    @Operation(summary = "修改菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @OperationLog(module = "系统管理", operation = "修改菜单")
    @PutMapping("/{id}")
    public Result<SysMenu> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        return Result.success(sysMenuService.update(menu));
    }

    /**
     * 删除菜单。
     */
    @Operation(summary = "删除菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @OperationLog(module = "系统管理", operation = "删除菜单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysMenuService.delete(id);
        return Result.success();
    }
}
