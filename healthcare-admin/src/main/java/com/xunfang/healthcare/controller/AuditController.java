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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 数据安全审计：操作日志、登录日志与导出。
 */
@Tag(name = "数据安全")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * 操作日志分页。
     */
    @Operation(summary = "操作日志分页")
    @PreAuthorize("@ss.hasPermi('audit:operation:list')")
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
    @PreAuthorize("@ss.hasPermi('audit:operation:list')")
    @GetMapping("/login/page")
    public Result<PageResult<SysLoginLog>> loginPage(PageQuery query,
                                                      @RequestParam(required = false) Integer status) {
        return Result.success(auditService.pageLoginLogs(query, status));
    }

    /**
     * 登录日志导出。
     */
    @Operation(summary = "登录日志导出")
    @PreAuthorize("@ss.hasPermi('audit:operation:list')")
    @GetMapping("/login/export")
    public ResponseEntity<Resource> exportLoginLogs(PageQuery query,
                                                     @RequestParam(required = false) Integer status) {
        PageResult<SysLoginLog> page = auditService.pageLoginLogs(query, status);
        StringBuilder csv = new StringBuilder();
        csv.append("username,ip,browser,os,status,msg,loginTime\n");
        for (SysLoginLog log : page.getList()) {
            csv.append(csv(log.getUsername())).append(',')
                    .append(csv(log.getIp())).append(',')
                    .append(csv(log.getBrowser())).append(',')
                    .append(csv(log.getOs())).append(',')
                    .append(csv(log.getStatus())).append(',')
                    .append(csv(log.getMsg())).append(',')
                    .append(csv(log.getLoginTime())).append('\n');
        }
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        String fileName = "login-log-" + System.currentTimeMillis() + ".csv";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(content));
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
