package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.dto.DispenseRequest;
import com.xunfang.healthcare.entity.BizPrescription;
import com.xunfang.healthcare.service.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 药房发药与退药。
 */
@Tag(name = "药房发药")
@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
public class PharmacyController {

    private final DrugService drugService;

    /**
     * 发药。
     */
    @Operation(summary = "发药")
    @PreAuthorize("@ss.hasPermi('drug:pharmacy:dispense')")
    @OperationLog(module = "药房管理", operation = "处方发药")
    @PostMapping("/dispense")
    public Result<BizPrescription> dispense(@RequestBody DispenseRequest request) {
        return Result.success(drugService.dispense(request));
    }

    /**
     * 退药。
     */
    @Operation(summary = "退药")
    @PreAuthorize("@ss.hasPermi('drug:pharmacy:return')")
    @OperationLog(module = "药房管理", operation = "处方退药")
    @PostMapping("/return")
    public Result<Map<String, Object>> returnDrug(@RequestBody DispenseRequest request) {
        return Result.success(drugService.returnDrug(request));
    }
}
