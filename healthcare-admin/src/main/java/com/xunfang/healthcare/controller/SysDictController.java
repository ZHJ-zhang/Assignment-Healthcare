package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysDictItem;
import com.xunfang.healthcare.entity.SysDictType;
import com.xunfang.healthcare.service.SysDictService;
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
 * 数据字典管理。
 */
@Tag(name = "字典管理")
@RestController
@RequestMapping("/api/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService sysDictService;

    /**
     * 字典类型列表。
     */
    @Operation(summary = "字典类型列表")
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/type/list")
    public Result<List<SysDictType>> listTypes() {
        return Result.success(sysDictService.listTypes());
    }

    /**
     * 字典项列表。
     */
    @Operation(summary = "字典项列表")
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/item/list")
    public Result<List<SysDictItem>> listItems(@RequestParam String dictType) {
        return Result.success(sysDictService.listAllItems(dictType));
    }

    /**
     * 按字典类型取启用中的字典项，供前端下拉框使用。
     */
    @Operation(summary = "启用中的字典项")
    @GetMapping("/data/{dictType}")
    public Result<List<SysDictItem>> data(@PathVariable String dictType) {
        return Result.success(sysDictService.listItems(dictType));
    }

    /**
     * 新增或修改字典类型。
     */
    @Operation(summary = "保存字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @OperationLog(module = "系统管理", operation = "保存字典类型")
    @PostMapping("/type")
    public Result<SysDictType> saveType(@RequestBody SysDictType type) {
        return Result.success(sysDictService.saveType(type));
    }

    /**
     * 修改字典类型。
     */
    @Operation(summary = "修改字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @OperationLog(module = "系统管理", operation = "修改字典类型")
    @PutMapping("/type/{id}")
    public Result<SysDictType> updateType(@PathVariable Long id, @RequestBody SysDictType type) {
        type.setId(id);
        return Result.success(sysDictService.saveType(type));
    }

    /**
     * 删除字典类型。
     */
    @Operation(summary = "删除字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @OperationLog(module = "系统管理", operation = "删除字典类型")
    @DeleteMapping("/type/{id}")
    public Result<Void> deleteType(@PathVariable Long id) {
        sysDictService.deleteType(id);
        return Result.success();
    }

    /**
     * 新增或修改字典项。
     */
    @Operation(summary = "保存字典项")
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @OperationLog(module = "系统管理", operation = "保存字典项")
    @PostMapping("/item")
    public Result<SysDictItem> saveItem(@RequestBody SysDictItem item) {
        return Result.success(sysDictService.saveItem(item));
    }

    /**
     * 修改字典项。
     */
    @Operation(summary = "修改字典项")
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @OperationLog(module = "系统管理", operation = "修改字典项")
    @PutMapping("/item/{id}")
    public Result<SysDictItem> updateItem(@PathVariable Long id, @RequestBody SysDictItem item) {
        item.setId(id);
        return Result.success(sysDictService.saveItem(item));
    }

    /**
     * 删除字典项。
     */
    @Operation(summary = "删除字典项")
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @OperationLog(module = "系统管理", operation = "删除字典项")
    @DeleteMapping("/item/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        sysDictService.deleteItem(id);
        return Result.success();
    }
}
