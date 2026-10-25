package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizFeeItem;
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

import java.util.List;

/**
 * 收费项目管理。
 */
@Tag(name = "收费项目管理")
@RestController
@RequestMapping("/api/fee-item")
@RequiredArgsConstructor
public class FeeItemController {

    private final ChargeService chargeService;

    /**
     * 收费项目分页。
     */
    @Operation(summary = "收费项目分页")
    @PreAuthorize("@ss.hasPermi('charge:feeItem:list')")
    @GetMapping("/page")
    public Result<PageResult<BizFeeItem>> page(PageQuery query,
                                                @RequestParam(required = false) Integer itemType,
                                                @RequestParam(required = false) Integer status) {
        return Result.success(chargeService.pageFeeItems(query, itemType, status));
    }

    /**
     * 启用中的收费项目列表。
     */
    @Operation(summary = "收费项目列表")
    @PreAuthorize("@ss.hasPermi('charge:feeItem:list')")
    @GetMapping("/list")
    public Result<List<BizFeeItem>> list(@RequestParam(required = false) Integer itemType) {
        return Result.success(chargeService.listFeeItems(itemType));
    }

    /**
     * 新增收费项目。
     */
    @Operation(summary = "新增收费项目")
    @PreAuthorize("@ss.hasPermi('charge:feeItem:edit')")
    @OperationLog(module = "收费财务", operation = "新增收费项目")
    @PostMapping
    public Result<BizFeeItem> create(@RequestBody BizFeeItem item) {
        return Result.success(chargeService.saveFeeItem(item));
    }

    /**
     * 修改收费项目。
     */
    @Operation(summary = "修改收费项目")
    @PreAuthorize("@ss.hasPermi('charge:feeItem:edit')")
    @OperationLog(module = "收费财务", operation = "修改收费项目")
    @PutMapping("/{id}")
    public Result<BizFeeItem> update(@PathVariable Long id, @RequestBody BizFeeItem item) {
        item.setId(id);
        return Result.success(chargeService.saveFeeItem(item));
    }
}
