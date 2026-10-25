package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizInvoice;
import com.xunfang.healthcare.service.ChargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发票打印与作废。
 */
@Tag(name = "发票管理")
@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final ChargeService chargeService;

    /**
     * 发票打印。
     */
    @Operation(summary = "发票打印")
    @PreAuthorize("@ss.hasPermi('charge:invoice:print')")
    @OperationLog(module = "收费财务", operation = "开具发票")
    @PostMapping("/print")
    public Result<BizInvoice> print(@RequestParam Long chargeId,
                                     @RequestParam(required = false) String title,
                                     @RequestParam(required = false) String taxNo,
                                     @RequestParam(required = false) Integer invoiceType) {
        return Result.success(chargeService.printInvoice(chargeId, title, taxNo, invoiceType));
    }

    /**
     * 发票分页。
     */
    @Operation(summary = "发票分页")
    @PreAuthorize("@ss.hasPermi('charge:invoice:list')")
    @GetMapping("/page")
    public Result<PageResult<BizInvoice>> page(PageQuery query,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) Long patientId) {
        return Result.success(chargeService.pageInvoices(query, status, patientId));
    }

    /**
     * 发票作废。
     */
    @Operation(summary = "发票作废")
    @PreAuthorize("@ss.hasPermi('charge:invoice:void')")
    @OperationLog(module = "收费财务", operation = "作废发票")
    @PutMapping("/{id}/void")
    public Result<BizInvoice> voidInvoice(@PathVariable Long id,
                                           @RequestParam(required = false) String reason) {
        return Result.success(chargeService.voidInvoice(id, reason));
    }
}
