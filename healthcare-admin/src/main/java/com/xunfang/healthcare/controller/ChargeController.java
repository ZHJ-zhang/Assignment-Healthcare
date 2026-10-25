package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.dto.ChargeRequest;
import com.xunfang.healthcare.entity.BizCharge;
import com.xunfang.healthcare.entity.BizInvoice;
import com.xunfang.healthcare.entity.BizRefund;
import com.xunfang.healthcare.service.ChargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 收费与退费。
 */
@Tag(name = "收费管理")
@RestController
@RequestMapping("/api/charge")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;

    /**
     * 收费。
     */
    @Operation(summary = "收费")
    @PreAuthorize("@ss.hasPermi('charge:add')")
    @OperationLog(module = "收费财务", operation = "收费")
    @PostMapping
    public Result<BizCharge> charge(@RequestBody ChargeRequest request) {
        return Result.success(chargeService.createCharge(request));
    }

    /**
     * 收费记录分页。
     */
    @Operation(summary = "收费记录分页")
    @PreAuthorize("@ss.hasPermi('charge:list')")
    @GetMapping("/page")
    public Result<PageResult<BizCharge>> page(PageQuery query,
                                              @RequestParam(required = false) Long patientId,
                                              @RequestParam(required = false) Integer payStatus,
                                              @RequestParam(required = false) Integer payType,
                                              @RequestParam(required = false) Long deptId,
                                              @RequestParam(required = false) Long visitId) {
        return Result.success(chargeService.pageCharges(query, patientId, payStatus, payType, deptId, visitId));
    }

    /**
     * 收费明细。
     */
    @Operation(summary = "收费明细")
    @PreAuthorize("@ss.hasPermi('charge:list')")
    @GetMapping("/{id}/detail")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(chargeService.chargeDetail(id));
    }

    /**
     * 申请退费。
     */
    @Operation(summary = "退费申请")
    @PreAuthorize("@ss.hasPermi('charge:refund')")
    @OperationLog(module = "收费财务", operation = "退费申请")
    @PostMapping("/{id}/refund")
    public Result<BizRefund> refund(@PathVariable Long id,
                                     @RequestParam(required = false) BigDecimal amount,
                                     @RequestParam(required = false) Integer refundType,
                                     @RequestParam(required = false) String reason) {
        return Result.success(chargeService.applyRefund(id, amount, refundType, reason));
    }

    /**
     * 退费记录分页。
     */
    @Operation(summary = "退费记录分页")
    @PreAuthorize("@ss.hasPermi('charge:refund:list')")
    @GetMapping("/refund/page")
    public Result<PageResult<BizRefund>> refundPage(PageQuery query,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Long patientId) {
        return Result.success(chargeService.pageRefunds(query, status, patientId));
    }

    /**
     * 审核退费。
     */
    @Operation(summary = "审核退费")
    @PreAuthorize("@ss.hasPermi('charge:refund')")
    @OperationLog(module = "收费财务", operation = "审核退费")
    @PutMapping("/refund/{id}/audit")
    public Result<BizRefund> auditRefund(@PathVariable Long id,
                                          @RequestParam(defaultValue = "true") boolean approved,
                                          @RequestParam(required = false) String reason) {
        return Result.success(chargeService.auditRefund(id, approved, reason));
    }

    /**
     * 执行退费。
     */
    @Operation(summary = "执行退费")
    @PreAuthorize("@ss.hasPermi('charge:refund')")
    @OperationLog(module = "收费财务", operation = "执行退费")
    @PutMapping("/refund/{id}/execute")
    public Result<BizRefund> executeRefund(@PathVariable Long id) {
        return Result.success(chargeService.refund(id));
    }
}
