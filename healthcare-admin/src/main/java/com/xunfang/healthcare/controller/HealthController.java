package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizHealthRecord;
import com.xunfang.healthcare.entity.BizInfectiousReport;
import com.xunfang.healthcare.entity.BizPhysicalExam;
import com.xunfang.healthcare.entity.BizVaccination;
import com.xunfang.healthcare.service.HealthService;
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

/**
 * 公共卫生：健康档案、预防接种、传染病与体检。
 */
@Tag(name = "公共卫生")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    /**
     * 健康档案分页。
     */
    @Operation(summary = "健康档案分页")
    @PreAuthorize("@ss.hasPermi('health:record:list')")
    @GetMapping("/record/page")
    public Result<PageResult<BizHealthRecord>> recordPage(PageQuery query,
                                                           @RequestParam(required = false) String community,
                                                           @RequestParam(required = false) Integer recordType) {
        return Result.success(healthService.pageRecords(query, community, recordType));
    }

    /**
     * 新建健康档案。
     */
    @Operation(summary = "新建健康档案")
    @PreAuthorize("@ss.hasPermi('health:record:add')")
    @OperationLog(module = "公共卫生", operation = "新建健康档案")
    @PostMapping("/record")
    public Result<BizHealthRecord> createRecord(@RequestBody BizHealthRecord record) {
        return Result.success(healthService.saveRecord(record));
    }

    /**
     * 更新健康档案。
     */
    @Operation(summary = "更新健康档案")
    @PreAuthorize("@ss.hasPermi('health:record:edit')")
    @OperationLog(module = "公共卫生", operation = "更新健康档案")
    @PutMapping("/record/{id}")
    public Result<BizHealthRecord> updateRecord(@PathVariable Long id, @RequestBody BizHealthRecord record) {
        record.setId(id);
        return Result.success(healthService.saveRecord(record));
    }

    /**
     * 删除健康档案。
     */
    @Operation(summary = "删除健康档案")
    @PreAuthorize("@ss.hasPermi('health:record:edit')")
    @OperationLog(module = "公共卫生", operation = "删除健康档案")
    @DeleteMapping("/record/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        healthService.deleteRecord(id);
        return Result.success();
    }

    /**
     * 接种记录分页。
     */
    @Operation(summary = "接种记录分页")
    @PreAuthorize("@ss.hasPermi('health:vaccination:list')")
    @GetMapping("/vaccination/page")
    public Result<PageResult<BizVaccination>> vaccinationPage(PageQuery query,
                                                               @RequestParam(required = false) Long patientId,
                                                               @RequestParam(required = false) Integer status) {
        return Result.success(healthService.pageVaccinations(query, patientId, status));
    }

    /**
     * 新增接种记录。
     */
    @Operation(summary = "新增接种记录")
    @PreAuthorize("@ss.hasPermi('health:vaccination:add')")
    @OperationLog(module = "公共卫生", operation = "新增接种记录")
    @PostMapping("/vaccination")
    public Result<BizVaccination> createVaccination(@RequestBody BizVaccination vaccination) {
        return Result.success(healthService.saveVaccination(vaccination));
    }

    /**
     * 接种提醒列表。
     */
    @Operation(summary = "接种提醒列表")
    @PreAuthorize("@ss.hasPermi('health:vaccination:list')")
    @GetMapping("/vaccination/remind")
    public Result<PageResult<BizVaccination>> vaccinationRemind(PageQuery query) {
        return Result.success(healthService.vaccinationRemind(query));
    }

    /**
     * 传染病上报。
     */
    @Operation(summary = "传染病上报")
    @PreAuthorize("@ss.hasPermi('health:infectious:add')")
    @OperationLog(module = "公共卫生", operation = "传染病上报")
    @PostMapping("/infectious/report")
    public Result<BizInfectiousReport> reportInfectious(@RequestBody BizInfectiousReport report) {
        return Result.success(healthService.saveInfectious(report));
    }

    /**
     * 传染病记录分页。
     */
    @Operation(summary = "传染病记录分页")
    @PreAuthorize("@ss.hasPermi('health:infectious:list')")
    @GetMapping("/infectious/page")
    public Result<PageResult<BizInfectiousReport>> infectiousPage(PageQuery query,
                                                                  @RequestParam(required = false) Integer status,
                                                                  @RequestParam(required = false) String diseaseName) {
        return Result.success(healthService.pageInfectious(query, status, diseaseName));
    }

    /**
     * 提交上报：0 待上报 → 1 已上报。
     */
    @Operation(summary = "提交传染病上报")
    @PreAuthorize("@ss.hasPermi('health:infectious:add')")
    @OperationLog(module = "公共卫生", operation = "提交传染病上报")
    @PutMapping("/infectious/{id}/submit")
    public Result<Void> submitInfectious(@PathVariable Long id) {
        healthService.submitInfectious(id);
        return Result.success();
    }

    /**
     * 审核报卡：1 已上报 → 2 已审核。
     */
    @Operation(summary = "审核传染病报卡")
    @PreAuthorize("@ss.hasPermi('health:infectious:audit')")
    @OperationLog(module = "公共卫生", operation = "审核传染病报卡")
    @PutMapping("/infectious/{id}/audit")
    public Result<Void> auditInfectious(@PathVariable Long id,
                                        @RequestParam(required = false) String remark) {
        healthService.auditInfectious(id, remark);
        return Result.success();
    }

    /**
     * 体检记录分页。
     */
    @Operation(summary = "体检记录分页")
    @PreAuthorize("@ss.hasPermi('health:checkup:list')")
    @GetMapping("/checkup/page")
    public Result<PageResult<BizPhysicalExam>> checkupPage(PageQuery query,
                                                            @RequestParam(required = false) Long patientId,
                                                            @RequestParam(required = false) Integer status) {
        return Result.success(healthService.pagePhysicalExams(query, patientId, status));
    }

    /**
     * 新增体检记录。
     */
    @Operation(summary = "新增体检记录")
    @PreAuthorize("@ss.hasPermi('health:checkup:add')")
    @OperationLog(module = "公共卫生", operation = "新增体检记录")
    @PostMapping("/checkup")
    public Result<BizPhysicalExam> createCheckup(@RequestBody BizPhysicalExam exam) {
        return Result.success(healthService.savePhysicalExam(exam));
    }
}
