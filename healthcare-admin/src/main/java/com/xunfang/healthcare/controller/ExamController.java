package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizCriticalValue;
import com.xunfang.healthcare.entity.BizExamApply;
import com.xunfang.healthcare.entity.BizExamReport;
import com.xunfang.healthcare.service.ExamService;
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

/**
 * 检查检验：申请、采样、报告与危急值。
 */
@Tag(name = "检查检验")
@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    /**
     * 开检查申请。
     */
    @Operation(summary = "开检查申请")
    @PreAuthorize("@ss.hasPermi('exam:apply:add')")
    @OperationLog(module = "检查检验", operation = "开检查申请")
    @PostMapping("/apply")
    public Result<BizExamApply> createApply(@RequestBody BizExamApply apply) {
        return Result.success(examService.createApply(apply));
    }

    /**
     * 申请分页。
     */
    @Operation(summary = "申请分页")
    @PreAuthorize("@ss.hasPermi('exam:apply:list')")
    @GetMapping("/apply/page")
    public Result<PageResult<BizExamApply>> applyPage(PageQuery query,
                                                      @RequestParam(required = false) Integer examType,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) Long patientId) {
        return Result.success(examService.pageApplies(query, examType, status, patientId));
    }

    /**
     * 确认缴费。
     */
    @Operation(summary = "确认缴费")
    @PreAuthorize("@ss.hasPermi('exam:apply:sample')")
    @OperationLog(module = "检查检验", operation = "检查申请缴费确认")
    @PutMapping("/apply/{id}/pay")
    public Result<Void> confirmPay(@PathVariable Long id) {
        examService.confirmPay(id);
        return Result.success();
    }

    /**
     * 样本采集。
     */
    @Operation(summary = "样本采集")
    @PreAuthorize("@ss.hasPermi('exam:apply:sample')")
    @OperationLog(module = "检查检验", operation = "样本采集")
    @PostMapping("/sample")
    public Result<Void> collectSample(@RequestParam Long id) {
        examService.collectSample(id);
        return Result.success();
    }

    /**
     * 录入检查结果。
     */
    @Operation(summary = "录入检查结果")
    @PreAuthorize("@ss.hasPermi('exam:report:add')")
    @OperationLog(module = "检查检验", operation = "录入检查结果")
    @PostMapping("/report")
    public Result<BizExamReport> saveReport(@RequestBody BizExamReport report) {
        return Result.success(examService.saveReport(report));
    }

    /**
     * 报告分页。
     */
    @Operation(summary = "报告分页")
    @PreAuthorize("@ss.hasPermi('exam:report:list')")
    @GetMapping("/report/page")
    public Result<PageResult<BizExamReport>> reportPage(PageQuery query,
                                                        @RequestParam(required = false) Long applyId,
                                                        @RequestParam(required = false) Integer status,
                                                        @RequestParam(required = false) Long patientId) {
        return Result.success(examService.pageReports(query, applyId, status, patientId));
    }

    /**
     * 报告详情。
     */
    @Operation(summary = "报告详情")
    @PreAuthorize("@ss.hasPermi('exam:report:list')")
    @GetMapping("/report/{id}")
    public Result<BizExamReport> reportDetail(@PathVariable Long id) {
        return Result.success(examService.reportDetail(id));
    }

    /**
     * 报告审核。
     */
    @Operation(summary = "报告审核")
    @PreAuthorize("@ss.hasPermi('exam:report:audit')")
    @OperationLog(module = "检查检验", operation = "报告审核")
    @PutMapping("/report/{id}/audit")
    public Result<Void> auditReport(@PathVariable Long id) {
        examService.auditReport(id);
        return Result.success();
    }

    /**
     * 报告发布。
     */
    @Operation(summary = "报告发布")
    @PreAuthorize("@ss.hasPermi('exam:report:publish')")
    @OperationLog(module = "检查检验", operation = "报告发布")
    @PutMapping("/report/{id}/publish")
    public Result<Void> publishReport(@PathVariable Long id) {
        examService.publishReport(id);
        return Result.success();
    }

    /**
     * 危急值分页。
     */
    @Operation(summary = "危急值分页")
    @PreAuthorize("@ss.hasPermi('exam:critical:list')")
    @GetMapping("/critical/page")
    public Result<PageResult<BizCriticalValue>> criticalPage(PageQuery query,
                                                              @RequestParam(required = false) Integer status) {
        return Result.success(examService.pageCritical(query, status));
    }

    /**
     * 危急值处理。
     */
    @Operation(summary = "危急值处理")
    @PreAuthorize("@ss.hasPermi('exam:critical:handle')")
    @OperationLog(module = "检查检验", operation = "危急值处理")
    @PostMapping("/critical/handle")
    public Result<Void> handleCritical(@RequestParam Long id,
                                        @RequestParam(required = false) String remark) {
        examService.handleCritical(id, remark);
        return Result.success();
    }
}
