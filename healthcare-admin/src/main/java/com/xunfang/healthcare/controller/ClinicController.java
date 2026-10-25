package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.dto.PrescriptionRequest;
import com.xunfang.healthcare.entity.BizMedicalRecord;
import com.xunfang.healthcare.entity.BizPrescription;
import com.xunfang.healthcare.entity.BizPrescriptionItem;
import com.xunfang.healthcare.entity.BizRegistration;
import com.xunfang.healthcare.service.ClinicService;
import com.xunfang.healthcare.service.RegistrationService;
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
import java.util.Map;

/**
 * 门诊接诊、病历与处方。
 */
@Tag(name = "门诊管理")
@RestController
@RequestMapping("/api/clinic")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;
    private final RegistrationService registrationService;

    /**
     * 就诊列表。
     */
    @Operation(summary = "就诊列表")
    @PreAuthorize("@ss.hasPermi('clinic:visit:list')")
    @GetMapping("/visit/list")
    public Result<List<BizRegistration>> visits(@RequestParam(required = false) Long deptId,
                                                 @RequestParam(required = false) Integer status) {
        return Result.success(clinicService.listVisits(deptId, status));
    }

    /**
     * 接诊。
     */
    @Operation(summary = "接诊")
    @PreAuthorize("@ss.hasPermi('clinic:visit:receive')")
    @OperationLog(module = "门诊管理", operation = "接诊")
    @PostMapping("/visit/{id}/receive")
    public Result<BizRegistration> receive(@PathVariable Long id) {
        return Result.success(registrationService.receive(id));
    }

    /**
     * 保存病历。
     */
    @Operation(summary = "保存病历")
    @PreAuthorize("@ss.hasPermi('clinic:record:save')")
    @OperationLog(module = "门诊管理", operation = "保存病历")
    @PostMapping("/record")
    public Result<BizMedicalRecord> saveRecord(@RequestBody BizMedicalRecord record) {
        return Result.success(clinicService.saveRecord(record));
    }

    /**
     * 提交病历。
     */
    @Operation(summary = "提交病历")
    @PreAuthorize("@ss.hasPermi('clinic:record:save')")
    @OperationLog(module = "门诊管理", operation = "提交病历")
    @PutMapping("/record/{id}/submit")
    public Result<Void> submitRecord(@PathVariable Long id) {
        clinicService.submitRecord(id);
        return Result.success();
    }

    /**
     * 归档病历。
     */
    @Operation(summary = "归档病历")
    @PreAuthorize("@ss.hasPermi('clinic:record:save')")
    @OperationLog(module = "门诊管理", operation = "归档病历")
    @PutMapping("/record/{id}/archive")
    public Result<Void> archiveRecord(@PathVariable Long id) {
        clinicService.archiveRecord(id);
        return Result.success();
    }

    /**
     * 病历分页。
     */
    @Operation(summary = "病历分页")
    @PreAuthorize("@ss.hasPermi('clinic:record:list')")
    @GetMapping("/record/page")
    public Result<PageResult<BizMedicalRecord>> recordPage(PageQuery query,
                                                            @RequestParam(required = false) Long patientId,
                                                            @RequestParam(required = false) Integer status,
                                                            @RequestParam(required = false) Long visitId) {
        return Result.success(clinicService.pageRecords(query, patientId, status, visitId));
    }
    /**
     * 处方分页。
     */
    @Operation(summary = "处方分页")
    @PreAuthorize("@ss.hasPermi('clinic:prescription:list')")
    @GetMapping("/prescription/page")
    public Result<PageResult<BizPrescription>> prescriptionPage(PageQuery query,
                                                                @RequestParam(required = false) Long patientId,
                                                                @RequestParam(required = false) Integer status,
                                                                @RequestParam(required = false) Long doctorId,
                                                                @RequestParam(required = false) Long visitId) {
        return Result.success(clinicService.pagePrescriptions(query, patientId, status, doctorId, visitId));
    }

    /**
     * 开处方。
     */
    @Operation(summary = "开处方")
    @PreAuthorize("@ss.hasPermi('clinic:prescription:add')")
    @OperationLog(module = "门诊管理", operation = "开处方")
    @PostMapping("/prescription")
    public Result<BizPrescription> createPrescription(@RequestBody PrescriptionRequest request) {
        return Result.success(clinicService.createPrescription(request));
    }

    /**
     * 处方详情。
     */
    @Operation(summary = "处方详情")
    @PreAuthorize("@ss.hasPermi('clinic:prescription:list')")
    @GetMapping("/prescription/{id}")
    public Result<Map<String, Object>> prescriptionDetail(@PathVariable Long id) {
        BizPrescription prescription = clinicService.prescriptionDetail(id);
        PageResult<BizPrescriptionItem> items = clinicService.prescriptionItems(
                new PageQuery(), id);
        return Result.success(Map.of("prescription", prescription, "items", items.getList()));
    }

    /**
     * 作废处方。
     */
    @Operation(summary = "作废处方")
    @PreAuthorize("@ss.hasPermi('clinic:prescription:remove')")
    @OperationLog(module = "门诊管理", operation = "作废处方")
    @DeleteMapping("/prescription/{id}")
    public Result<Void> voidPrescription(@PathVariable Long id) {
        clinicService.voidPrescription(id);
        return Result.success();
    }
}
