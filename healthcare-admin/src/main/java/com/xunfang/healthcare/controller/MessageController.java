package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysMessage;
import com.xunfang.healthcare.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内消息。
 */
@Tag(name = "消息通知")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 消息分页。
     */
    @Operation(summary = "消息分页")
    @PreAuthorize("@ss.hasPermi('message:list')")
    @GetMapping("/page")
    public Result<PageResult<SysMessage>> page(PageQuery query,
                                                @RequestParam(required = false) Integer isRead,
                                                @RequestParam(required = false) Long receiverId) {
        return Result.success(messageService.pageMessages(query, isRead, receiverId));
    }

    /**
     * 未读数量。
     */
    @Operation(summary = "未读消息数量")
    @GetMapping("/unread")
    public Result<Long> unread() {
        return Result.success(messageService.unreadCount());
    }

    /**
     * 标记已读。
     */
    @Operation(summary = "标记已读")
    @PreAuthorize("@ss.hasPermi('message:read')")
    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        messageService.readMessage(id);
        return Result.success();
    }

    /**
     * 全部标记已读。
     */
    @Operation(summary = "全部标记已读")
    @PreAuthorize("@ss.hasPermi('message:read')")
    @PutMapping("/read-all")
    public Result<Integer> readAll() {
        return Result.success(messageService.readAll());
    }
}
