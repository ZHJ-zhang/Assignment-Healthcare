package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysSmsRecord;
import com.xunfang.healthcare.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短信发送与记录。
 */
@Tag(name = "短信通知")
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final MessageService messageService;

    /**
     * 发送短信。
     */
    @Operation(summary = "发送短信")
    @PreAuthorize("@ss.hasPermi('message:sms:send')")
    @OperationLog(module = "消息通知", operation = "发送短信", saveParams = false)
    @PostMapping("/send")
    public Result<SysSmsRecord> send(@RequestParam String phone,
                                      @RequestParam(required = false) String templateCode,
                                      @RequestParam String content) {
        return Result.success(messageService.sendSms(phone, templateCode, content));
    }

    /**
     * 短信记录分页。
     */
    @Operation(summary = "短信记录分页")
    @PreAuthorize("@ss.hasPermi('message:sms:list')")
    @GetMapping("/page")
    public Result<PageResult<SysSmsRecord>> page(PageQuery query,
                                                 @RequestParam(required = false) Integer sendStatus) {
        return Result.success(messageService.pageSms(query, sendStatus));
    }
}
