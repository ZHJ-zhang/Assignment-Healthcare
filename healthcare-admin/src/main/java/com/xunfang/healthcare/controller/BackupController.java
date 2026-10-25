package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysBackup;
import com.xunfang.healthcare.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据备份与恢复。
 */
@Tag(name = "数据备份")
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final AuditService auditService;

    /**
     * 手动备份。
     */
    @Operation(summary = "手动备份")
    @PreAuthorize("@ss.hasPermi('audit:backup:add')")
    @OperationLog(module = "数据安全", operation = "手动备份")
    @PostMapping
    public Result<SysBackup> backup(@RequestBody(required = false) java.util.Map<String, Object> body) {
        String backupName = body == null ? null : (String) body.get("backupName");
        Integer backupType = body == null ? null : (Integer) body.get("backupType");
        return Result.success(auditService.createBackup(backupName, backupType));
    }

    /**
     * 备份记录分页。
     */
    @Operation(summary = "备份记录分页")
    @PreAuthorize("@ss.hasPermi('audit:backup:list')")
    @GetMapping("/page")
    public Result<PageResult<SysBackup>> page(PageQuery query,
                                               @RequestParam(required = false) Integer status) {
        return Result.success(auditService.pageBackups(query, status));
    }

    /**
     * 数据恢复。
     */
    @Operation(summary = "数据恢复")
    @PreAuthorize("@ss.hasPermi('audit:backup:restore')")
    @OperationLog(module = "数据安全", operation = "数据恢复")
    @PostMapping("/{id}/restore")
    public Result<SysBackup> restore(@PathVariable Long id) {
        return Result.success(auditService.restoreBackup(id));
    }

    /**
     * 删除备份。
     */
    @Operation(summary = "删除备份")
    @PreAuthorize("@ss.hasPermi('audit:backup:remove')")
    @OperationLog(module = "数据安全", operation = "删除备份")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        auditService.deleteBackup(id);
        return Result.success();
    }
}
