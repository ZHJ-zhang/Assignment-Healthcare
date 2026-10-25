package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysDept;
import com.xunfang.healthcare.service.SysDeptService;
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
 * 科室管理。
 */
@Tag(name = "科室管理")
@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService sysDeptService;

    /**
     * 科室列表。
     */
    @Operation(summary = "科室列表")
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list")
    public Result<List<SysDept>> list() {
        return Result.success(sysDeptService.list());
    }

    /**
     * 科室树。
     */
    @Operation(summary = "科室树")
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/tree")
    public Result<List<SysDept>> tree() {
        return Result.success(sysDeptService.tree());
    }

    /**
     * 科室详情。
     */
    @Operation(summary = "科室详情")
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/{id}")
    public Result<SysDept> detail(@PathVariable Long id) {
        return Result.success(sysDeptService.detail(id));
    }

    /**
     * 新增科室。
     */
    @Operation(summary = "新增科室")
    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @OperationLog(module = "系统管理", operation = "新增科室")
    @PostMapping
    public Result<SysDept> create(@RequestBody SysDept dept) {
        return Result.success(sysDeptService.create(dept));
    }

    /**
     * 修改科室。
     */
    @Operation(summary = "修改科室")
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @OperationLog(module = "系统管理", operation = "修改科室")
    @PutMapping("/{id}")
    public Result<SysDept> update(@PathVariable Long id, @RequestBody SysDept dept) {
        dept.setId(id);
        return Result.success(sysDeptService.update(dept));
    }

    /**
     * 删除科室。
     */
    @Operation(summary = "删除科室")
    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @OperationLog(module = "系统管理", operation = "删除科室")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysDeptService.delete(id);
        return Result.success();
    }
}
