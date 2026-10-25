package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizSettlement;
import com.xunfang.healthcare.service.ChargeService;
import com.xunfang.healthcare.service.InpatientService;
import com.xunfang.healthcare.vo.FinanceSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务日结月结与医保结算。
 */
@Tag(name = "财务管理")
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final ChargeService chargeService;
    private final InpatientService inpatientService;

    /**
     * 日结。
     */
    @Operation(summary = "日结")
    @PreAuthorize("@ss.hasPermi('charge:finance:daily')")
    @GetMapping("/daily")
    public Result<FinanceSummaryVO> daily(@RequestParam(required = false) String date) {
        String day = date == null || date.isBlank() ? LocalDate.now().toString() : date;
        return Result.success(chargeService.summary(day, day));
    }

    /**
     * 月结。
     */
    @Operation(summary = "月结")
    @PreAuthorize("@ss.hasPermi('charge:finance:daily')")
    @GetMapping("/monthly")
    public Result<FinanceSummaryVO> monthly(@RequestParam(required = false) String month) {
        LocalDate target = month == null || month.isBlank()
                ? LocalDate.now().withDayOfMonth(1)
                : LocalDate.parse(month + "-01");
        LocalDate end = target.withDayOfMonth(target.lengthOfMonth());
        return Result.success(chargeService.summary(target.toString(), end.toString()));
    }

    /**
     * 医保结算。
     */
    @Operation(summary = "医保结算")
    @PreAuthorize("@ss.hasPermi('charge:finance:daily')")
    @OperationLog(module = "收费财务", operation = "医保结算")
    @PostMapping("/insurance/settle")
    public Result<BizSettlement> insuranceSettle(@RequestParam Long admissionId,
                                                 @RequestParam BigDecimal insuranceAmount,
                                                 @RequestParam BigDecimal selfAmount) {
        return Result.success(inpatientService.settle(admissionId, insuranceAmount, selfAmount));
    }
}
