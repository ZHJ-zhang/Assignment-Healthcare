package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 预警列表：危急值与库存预警的合并视图。
 */
@Tag(name = "预警管理")
@RestController
@RequestMapping("/api/warning")
@RequiredArgsConstructor
public class WarningController {

    private final MessageService messageService;

    /**
     * 预警列表。
     */
    @Operation(summary = "预警列表")
    @PreAuthorize("@ss.hasPermi('message:list')")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(defaultValue = "50") int limit) {
        return Result.success(messageService.warningList(limit));
    }
}
