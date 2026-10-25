package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysLoginLog;
import com.xunfang.healthcare.entity.SysOperationLog;
import com.xunfang.healthcare.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统日志查询。
 */
@Tag(name = "日志管理")
@RestController
@RequestMapping("/api/system/log")
@RequiredArgsConstructor
public class SysLogController {

    private final AuditService auditService;

    /**
     * 操作日志分页。
     */
    @Operation(summary = "操作日志分页")
    @PreAuthorize("@ss.hasPermi('system:log:list')")
    @GetMapping("/operation/page")
    public Result<PageResult<SysOperationLog>> operationPage(PageQuery query,
                                                              @RequestParam(required = false) String module,
                                                              @RequestParam(required = false) Integer status) {
        return Result.success(auditService.pageOperationLogs(query, module, status));
    }

    /**
     * 登录日志分页。
     */
    @Operation(summary = "登录日志分页")
    @PreAuthorize("@ss.hasPermi('system:log:loginList')")
    @GetMapping("/login/page")
    public Result<PageResult<SysLoginLog>> loginPage(PageQuery query,
                                                      @RequestParam(required = false) Integer status) {
        return Result.success(auditService.pageLoginLogs(query, status));
    }
}
