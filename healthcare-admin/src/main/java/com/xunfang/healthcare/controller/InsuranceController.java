package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizSettlement;
import com.xunfang.healthcare.service.InpatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 医保结算。
 */
@Tag(name = "医保结算")
@RestController
@RequestMapping("/api/insurance")
@RequiredArgsConstructor
public class InsuranceController {

    private final InpatientService inpatientService;

    /**
     * 医保结算。
     */
    @Operation(summary = "医保结算")
    @PreAuthorize("@ss.hasPermi('charge:finance:daily')")
    @OperationLog(module = "收费财务", operation = "医保结算")
    @PostMapping("/settle")
    public Result<BizSettlement> settle(@RequestParam Long admissionId,
                                         @RequestParam BigDecimal insuranceAmount,
                                         @RequestParam BigDecimal selfAmount) {
        return Result.success(inpatientService.settle(admissionId, insuranceAmount, selfAmount));
    }
}
