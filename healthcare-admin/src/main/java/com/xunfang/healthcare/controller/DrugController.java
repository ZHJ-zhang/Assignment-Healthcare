package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.dto.StockRequest;
import com.xunfang.healthcare.entity.BizDrug;
import com.xunfang.healthcare.entity.BizStock;
import com.xunfang.healthcare.entity.BizStockRecord;
import com.xunfang.healthcare.entity.BizWarehouse;
import com.xunfang.healthcare.service.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 药品目录、库存与库房。
 */
@Tag(name = "药房管理")
@RestController
@RequestMapping("/api/drug")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService drugService;

    /**
     * 药品分页。
     */
    @Operation(summary = "药品分页")
    @PreAuthorize("@ss.hasPermi('drug:list')")
    @GetMapping("/page")
    public Result<PageResult<BizDrug>> page(PageQuery query,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) Integer isSpecial) {
        return Result.success(drugService.pageDrugs(query, category, isSpecial));
    }

    /**
     * 药品详情。
     */
    @Operation(summary = "药品详情")
    @PreAuthorize("@ss.hasPermi('drug:list')")
    @GetMapping("/{id}")
    public Result<BizDrug> detail(@PathVariable Long id) {
        return Result.success(drugService.drugDetail(id));
    }

    /**
     * 新增药品。
     */
    @Operation(summary = "新增药品")
    @PreAuthorize("@ss.hasPermi('drug:add')")
    @OperationLog(module = "药房管理", operation = "新增药品")
    @PostMapping
    public Result<BizDrug> create(@RequestBody BizDrug drug) {
        return Result.success(drugService.saveDrug(drug));
    }

    /**
     * 修改药品。
     */
    @Operation(summary = "修改药品")
    @PreAuthorize("@ss.hasPermi('drug:edit')")
    @OperationLog(module = "药房管理", operation = "修改药品")
    @PutMapping("/{id}")
    public Result<BizDrug> update(@PathVariable Long id, @RequestBody BizDrug drug) {
        drug.setId(id);
        return Result.success(drugService.saveDrug(drug));
    }

    /**
     * 删除药品。
     */
    @Operation(summary = "删除药品")
    @PreAuthorize("@ss.hasPermi('drug:remove')")
    @OperationLog(module = "药房管理", operation = "删除药品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        drugService.deleteDrug(id);
        return Result.success();
    }

    /**
     * 库存分页。
     */
    @Operation(summary = "库存分页")
    @PreAuthorize("@ss.hasPermi('drug:stock:list')")
    @GetMapping("/stock/page")
    public Result<PageResult<BizStock>> stockPage(PageQuery query,
                                                  @RequestParam(required = false) Long drugId,
                                                  @RequestParam(required = false) Long warehouseId) {
        return Result.success(drugService.pageStock(query, drugId, warehouseId));
    }

    /**
     * 药品入库。
     */
    @Operation(summary = "药品入库")
    @PreAuthorize("@ss.hasPermi('drug:stock:in')")
    @OperationLog(module = "药房管理", operation = "药品入库")
    @PostMapping("/stock/in")
    public Result<BizStock> stockIn(@RequestBody StockRequest request) {
        return Result.success(drugService.stockIn(request));
    }

    /**
     * 药品出库。
     */
    @Operation(summary = "药品出库")
    @PreAuthorize("@ss.hasPermi('drug:stock:out')")
    @OperationLog(module = "药房管理", operation = "药品出库")
    @PostMapping("/stock/out")
    public Result<BizStock> stockOut(@RequestBody StockRequest request) {
        return Result.success(drugService.stockOut(request));
    }

    /**
     * 库存预警列表。
     */
    @Operation(summary = "库存预警列表")
    @PreAuthorize("@ss.hasPermi('drug:stock:list')")
    @GetMapping("/stock/warn")
    public Result<List<BizStock>> warnList() {
        return Result.success(drugService.warningList());
    }

    /**
     * 效期预警列表。
     */
    @Operation(summary = "效期预警列表")
    @PreAuthorize("@ss.hasPermi('drug:stock:list')")
    @GetMapping("/stock/expire")
    public Result<List<BizStock>> expireList(@RequestParam(defaultValue = "90") int warnDays) {
        return Result.success(drugService.expireList(warnDays));
    }

    /**
     * 出入库流水分页。
     */
    @Operation(summary = "出入库流水分页")
    @PreAuthorize("@ss.hasPermi('drug:stock:list')")
    @GetMapping("/stock/record/page")
    public Result<PageResult<BizStockRecord>> stockRecordPage(PageQuery query,
                                                              @RequestParam(required = false) Long drugId) {
        return Result.success(drugService.pageStockRecords(query, drugId));
    }

    /**
     * 库房列表。
     */
    @Operation(summary = "库房列表")
    @PreAuthorize("@ss.hasPermi('drug:warehouse:list')")
    @GetMapping("/warehouse/list")
    public Result<List<BizWarehouse>> warehouses() {
        return Result.success(drugService.listWarehouses());
    }

    /**
     * 新增库房。
     */
    @Operation(summary = "新增库房")
    @PreAuthorize("@ss.hasPermi('drug:warehouse:list')")
    @OperationLog(module = "药房管理", operation = "新增库房")
    @PostMapping("/warehouse")
    public Result<BizWarehouse> saveWarehouse(@RequestBody BizWarehouse warehouse) {
        return Result.success(drugService.saveWarehouse(warehouse));
    }
}
