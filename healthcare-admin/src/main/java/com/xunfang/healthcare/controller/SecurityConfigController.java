package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysSecurityConfig;
import com.xunfang.healthcare.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 安全策略配置。
 */
@Tag(name = "安全配置")
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityConfigController {

    private final AuditService auditService;

    /**
     * 查询安全配置。
     */
    @Operation(summary = "查询安全配置")
    @PreAuthorize("@ss.hasPermi('audit:security:view')")
    @GetMapping("/config")
    public Result<SysSecurityConfig> config() {
        return Result.success(auditService.securityConfig());
    }

    /**
     * 更新安全配置。
     */
    @Operation(summary = "更新安全配置")
    @PreAuthorize("@ss.hasPermi('audit:security:edit')")
    @OperationLog(module = "数据安全", operation = "更新安全配置")
    @PutMapping("/config")
    public Result<SysSecurityConfig> saveConfig(@RequestBody SysSecurityConfig config) {
        return Result.success(auditService.saveSecurityConfig(config));
    }
}
