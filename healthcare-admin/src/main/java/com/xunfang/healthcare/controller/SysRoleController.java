package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.dto.IdListRequest;
import com.xunfang.healthcare.entity.SysRole;
import com.xunfang.healthcare.service.SysRoleService;
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
import java.util.Map;

/**
 * 角色管理与权限分配。
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    /**
     * 角色列表。
     */
    @Operation(summary = "角色列表")
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.success(sysRoleService.list());
    }

    /**
     * 角色分页。
     */
    @Operation(summary = "角色分页")
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/page")
    public Result<PageResult<SysRole>> page(PageQuery query) {
        return Result.success(sysRoleService.page(query));
    }

    /**
     * 角色详情。
     */
    @Operation(summary = "角色详情")
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        SysRole role = sysRoleService.detail(id);
        List<Long> menuIds = sysRoleService.listMenuIds(id);
        return Result.success(Map.of("role", role, "menuIds", menuIds));
    }

    /**
     * 新增角色。
     */
    @Operation(summary = "新增角色")
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    @OperationLog(module = "系统管理", operation = "新增角色")
    @PostMapping
    public Result<SysRole> create(@RequestBody SysRole role) {
        return Result.success(sysRoleService.create(role));
    }

    /**
     * 修改角色。
     */
    @Operation(summary = "修改角色")
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @OperationLog(module = "系统管理", operation = "修改角色")
    @PutMapping("/{id}")
    public Result<SysRole> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        return Result.success(sysRoleService.update(role));
    }

    /**
     * 删除角色。
     */
    @Operation(summary = "删除角色")
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @OperationLog(module = "系统管理", operation = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.delete(id);
        return Result.success();
    }

    /**
     * 分配菜单权限。
     */
    @Operation(summary = "分配权限")
    @PreAuthorize("@ss.hasPermi('system:role:permission')")
    @OperationLog(module = "系统管理", operation = "分配角色权限")
    @PutMapping("/{id}/permission")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody IdListRequest request) {
        sysRoleService.assignPermissions(id, request.getIds());
        return Result.success();
    }
}
