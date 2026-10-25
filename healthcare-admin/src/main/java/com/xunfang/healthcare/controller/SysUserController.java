package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.dto.PasswordRequest;
import com.xunfang.healthcare.dto.UserSaveRequest;
import com.xunfang.healthcare.entity.SysUser;
import com.xunfang.healthcare.service.SysUserService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户管理。
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 用户分页。
     */
    @Operation(summary = "用户分页")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(PageQuery query,
                                            @RequestParam(required = false) Long deptId,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(sysUserService.page(query, deptId, status));
    }

    /**
     * 用户详情。
     */
    @Operation(summary = "用户详情")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        SysUser user = sysUserService.detail(id);
        List<Long> roleIds = sysUserService.listRoleIds(id);
        return Result.success(Map.of("user", user, "roleIds", roleIds));
    }

    /**
     * 新增用户。
     */
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @OperationLog(module = "系统管理", operation = "新增用户")
    @PostMapping
    public Result<SysUser> create(@RequestBody UserSaveRequest request) {
        return Result.success(sysUserService.create(request, request.getRoleIds(), request.getRawPassword()));
    }

    /**
     * 修改用户。
     */
    @Operation(summary = "修改用户")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @OperationLog(module = "系统管理", operation = "修改用户")
    @PutMapping("/{id}")
    public Result<SysUser> update(@PathVariable Long id, @RequestBody UserSaveRequest request) {
        request.setId(id);
        return Result.success(sysUserService.update(request, request.getRoleIds()));
    }

    /**
     * 删除用户。
     */
    @Operation(summary = "删除用户")
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @OperationLog(module = "系统管理", operation = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.success();
    }

    /**
     * 启用或禁用用户。
     */
    @Operation(summary = "启用/禁用用户")
    @PreAuthorize("@ss.hasPermi('system:user:status')")
    @OperationLog(module = "系统管理", operation = "修改用户状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        sysUserService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 重置密码。
     */
    @Operation(summary = "重置密码")
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @OperationLog(module = "系统管理", operation = "重置密码", saveParams = false)
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        sysUserService.resetPassword(id, password);
        return Result.success();
    }

    /**
     * 修改自己的密码。
     */
    @Operation(summary = "修改密码")
    @OperationLog(module = "系统管理", operation = "修改密码", saveParams = false)
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody PasswordRequest request) {
        sysUserService.changePassword(request);
        return Result.success();
    }
}
