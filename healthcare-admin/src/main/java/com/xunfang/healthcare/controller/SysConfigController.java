package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysConfig;
import com.xunfang.healthcare.service.SysConfigService;
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
 * 系统参数配置。
 */
@Tag(name = "参数配置")
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    /**
     * 参数分页。
     */
    @Operation(summary = "参数分页")
    @PreAuthorize("@ss.hasPermi('system:config:list')")
    @GetMapping("/page")
    public Result<PageResult<SysConfig>> page(PageQuery query) {
        return Result.success(sysConfigService.page(query));
    }

    /**
     * 全部参数列表。
     */
    @Operation(summary = "参数列表")
    @PreAuthorize("@ss.hasPermi('system:config:list')")
    @GetMapping("/list")
    public Result<List<SysConfig>> list() {
        return Result.success(sysConfigService.list());
    }

    /**
     * 新增或修改参数。
     */
    @Operation(summary = "保存参数")
    @PreAuthorize("@ss.hasPermi('system:config:add')")
    @OperationLog(module = "系统管理", operation = "保存系统参数")
    @PostMapping
    public Result<SysConfig> save(@RequestBody SysConfig config) {
        return Result.success(sysConfigService.save(config));
    }

    /**
     * 修改参数。
     */
    @Operation(summary = "修改参数")
    @PreAuthorize("@ss.hasPermi('system:config:edit')")
    @OperationLog(module = "系统管理", operation = "修改系统参数")
    @PutMapping("/{id}")
    public Result<SysConfig> update(@PathVariable Long id, @RequestBody SysConfig config) {
        config.setId(id);
        return Result.success(sysConfigService.save(config));
    }

    /**
     * 删除参数。
     */
    @Operation(summary = "删除参数")
    @PreAuthorize("@ss.hasPermi('system:config:remove')")
    @OperationLog(module = "系统管理", operation = "删除系统参数")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysConfigService.delete(id);
        return Result.success();
    }
}
