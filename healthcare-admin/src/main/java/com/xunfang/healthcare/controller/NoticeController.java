package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysNotice;
import com.xunfang.healthcare.service.MessageService;
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
 * 公告通知。
 */
@Tag(name = "公告通知")
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final MessageService messageService;

    /**
     * 发布公告。
     */
    @Operation(summary = "发布公告")
    @PreAuthorize("@ss.hasPermi('message:notice:add')")
    @OperationLog(module = "消息通知", operation = "发布公告")
    @PostMapping
    public Result<SysNotice> publish(@RequestBody SysNotice notice) {
        return Result.success(messageService.publishNotice(notice));
    }

    /**
     * 公告分页。
     */
    @Operation(summary = "公告分页")
    @PreAuthorize("@ss.hasPermi('message:notice:list')")
    @GetMapping("/page")
    public Result<PageResult<SysNotice>> page(PageQuery query,
                                               @RequestParam(required = false) Integer noticeType,
                                               @RequestParam(required = false) Integer status) {
        return Result.success(messageService.pageNotices(query, noticeType, status));
    }

    /**
     * 删除公告。
     */
    @Operation(summary = "删除公告")
    @PreAuthorize("@ss.hasPermi('message:notice:remove')")
    @OperationLog(module = "消息通知", operation = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.deleteNotice(id);
        return Result.success();
    }
}
