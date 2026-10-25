package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.SysReportConfig;
import com.xunfang.healthcare.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 统计报表查询与导出。
 */
@Tag(name = "统计报表")
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 门诊统计。
     */
    @Operation(summary = "门诊统计")
    @PreAuthorize("@ss.hasPermi('report:outpatient')")
    @GetMapping("/outpatient")
    public Result<Map<String, Object>> outpatient(@RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate) {
        return Result.success(reportService.outpatientReport(startDate, endDate));
    }

    /**
     * 住院统计。
     */
    @Operation(summary = "住院统计")
    @PreAuthorize("@ss.hasPermi('report:inpatient')")
    @GetMapping("/inpatient")
    public Result<Map<String, Object>> inpatient(@RequestParam(required = false) String startDate,
                                                  @RequestParam(required = false) String endDate) {
        return Result.success(reportService.inpatientReport(startDate, endDate));
    }

    /**
     * 收入统计。
     */
    @Operation(summary = "收入统计")
    @PreAuthorize("@ss.hasPermi('report:income')")
    @GetMapping("/income")
    public Result<Map<String, Object>> income(@RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate) {
        return Result.success(reportService.incomeReport(startDate, endDate));
    }

    /**
     * 药品统计。
     */
    @Operation(summary = "药品统计")
    @PreAuthorize("@ss.hasPermi('report:drug')")
    @GetMapping("/drug")
    public Result<Map<String, Object>> drug(@RequestParam(required = false) String startDate,
                                             @RequestParam(required = false) String endDate) {
        return Result.success(reportService.drugReport(startDate, endDate));
    }

    /**
     * 患者分析。
     */
    @Operation(summary = "患者分析")
    @PreAuthorize("@ss.hasPermi('report:patient')")
    @GetMapping("/patient")
    public Result<Map<String, Object>> patient(@RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        return Result.success(reportService.patientReport(startDate, endDate));
    }

    /**
     * 自定义报表配置列表。
     */
    @Operation(summary = "自定义报表配置")
    @PreAuthorize("@ss.hasPermi('report:export')")
    @GetMapping("/custom/list")
    public Result<List<SysReportConfig>> customList() {
        return Result.success(reportService.listCustomReports());
    }

    /**
     * 保存自定义报表配置。
     */
    @Operation(summary = "保存自定义报表")
    @PreAuthorize("@ss.hasPermi('report:export')")
    @OperationLog(module = "统计报表", operation = "保存自定义报表")
    @PostMapping("/custom")
    public Result<SysReportConfig> saveCustom(@RequestBody SysReportConfig config) {
        return Result.success(reportService.saveCustomReport(config));
    }

    /**
     * 执行自定义报表。
     */
    @Operation(summary = "执行自定义报表")
    @PreAuthorize("@ss.hasPermi('report:export')")
    @PostMapping("/custom/{id}/execute")
    public Result<Map<String, Object>> executeCustom(@PathVariable Long id,
                                                     @RequestParam(required = false) String startDate,
                                                     @RequestParam(required = false) String endDate) {
        return Result.success(reportService.executeCustomReport(id, startDate, endDate));
    }

    /**
     * 导出 Excel 或 PDF。
     */
    @Operation(summary = "导出报表")
    @PreAuthorize("@ss.hasPermi('report:export')")
    @GetMapping("/export")
    public ResponseEntity<Resource> export(@RequestParam(defaultValue = "outpatient") String type,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate,
                                            @RequestParam(defaultValue = "excel") String format) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] content = pdf
                ? reportService.exportPdf(type, startDate, endDate)
                : reportService.exportExcel(type, startDate, endDate);
        String fileName = type + "-" + System.currentTimeMillis() + (pdf ? ".pdf" : ".xlsx");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        headers.setContentType(pdf ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(content));
    }
}
