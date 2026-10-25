package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizFollowUp;
import com.xunfang.healthcare.entity.BizMedicalRecord;
import com.xunfang.healthcare.entity.BizPatient;
import com.xunfang.healthcare.entity.BizPatientCard;
import com.xunfang.healthcare.service.PatientService;
import com.xunfang.healthcare.vo.PatientVO;
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
 * 患者档案、就诊卡、随访与病历。
 */
@Tag(name = "患者管理")
@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    /**
     * 患者分页。
     */
    @Operation(summary = "患者分页")
    @PreAuthorize("@ss.hasPermi('patient:list')")
    @GetMapping("/page")
    public Result<PageResult<PatientVO>> page(PageQuery query,
                                              @RequestParam(required = false) String gender,
                                              @RequestParam(required = false) Integer status) {
        return Result.success(patientService.page(query, gender, status));
    }

    /**
     * 患者详情。
     */
    @Operation(summary = "患者详情")
    @PreAuthorize("@ss.hasPermi('patient:list')")
    @GetMapping("/{id}")
    public Result<PatientVO> detail(@PathVariable Long id) {
        return Result.success(patientService.detail(id));
    }

    /**
     * 新增患者。
     */
    @Operation(summary = "新增患者")
    @PreAuthorize("@ss.hasPermi('patient:add')")
    @OperationLog(module = "患者管理", operation = "新增患者")
    @PostMapping
    public Result<BizPatient> create(@RequestBody BizPatient patient) {
        return Result.success(patientService.create(patient));
    }

    /**
     * 修改患者。
     */
    @Operation(summary = "修改患者")
    @PreAuthorize("@ss.hasPermi('patient:edit')")
    @OperationLog(module = "患者管理", operation = "修改患者")
    @PutMapping("/{id}")
    public Result<BizPatient> update(@PathVariable Long id, @RequestBody BizPatient patient) {
        patient.setId(id);
        return Result.success(patientService.update(patient));
    }

    /**
     * 删除患者。
     */
    @Operation(summary = "删除患者")
    @PreAuthorize("@ss.hasPermi('patient:remove')")
    @OperationLog(module = "患者管理", operation = "删除患者")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        patientService.delete(id);
        return Result.success();
    }

    /**
     * 患者病历列表。
     */
    @Operation(summary = "患者病历列表")
    @PreAuthorize("@ss.hasPermi('patient:record:list')")
    @GetMapping("/{id}/records")
    public Result<List<BizMedicalRecord>> records(@PathVariable Long id) {
        return Result.success(patientService.listRecords(id));
    }

    /**
     * 保存病历。
     */
    @Operation(summary = "保存病历")
    @PreAuthorize("@ss.hasPermi('patient:record:list')")
    @OperationLog(module = "患者管理", operation = "保存病历")
    @PostMapping("/record")
    public Result<BizMedicalRecord> saveRecord(@RequestBody BizMedicalRecord record) {
        return Result.success(patientService.saveRecord(record));
    }

    /**
     * 就诊卡列表。
     */
    @Operation(summary = "就诊卡列表")
    @PreAuthorize("@ss.hasPermi('patient:card:list')")
    @GetMapping("/card/list")
    public Result<List<BizPatientCard>> cardList(@RequestParam Long patientId) {
        return Result.success(patientService.listCards(patientId));
    }

    /**
     * 为患者发放就诊卡。
     */
    @Operation(summary = "发放就诊卡")
    @PreAuthorize("@ss.hasPermi('patient:card:add')")
    @OperationLog(module = "患者管理", operation = "发放就诊卡")
    @PostMapping("/card")
    public Result<BizPatientCard> issueCard(@RequestBody BizPatientCard card) {
        return Result.success(patientService.issueCard(card));
    }

    /**
     * 补办就诊卡。
     */
    @Operation(summary = "补办就诊卡")
    @PreAuthorize("@ss.hasPermi('patient:card:reissue')")
    @OperationLog(module = "患者管理", operation = "补办就诊卡")
    @PostMapping("/card/reissue")
    public Result<BizPatientCard> reissue(@RequestBody BizPatientCard card) {
        return Result.success(patientService.reissueCard(card.getId(), card.getRemark()));
    }

    /**
     * 挂失就诊卡。
     */
    @Operation(summary = "就诊卡挂失")
    @PreAuthorize("@ss.hasPermi('patient:card:loss')")
    @OperationLog(module = "患者管理", operation = "就诊卡挂失")
    @PutMapping("/card/{id}/loss")
    public Result<Void> lossCard(@PathVariable Long id) {
        patientService.lossCard(id);
        return Result.success();
    }

    /**
     * 注销就诊卡。
     */
    @Operation(summary = "注销就诊卡")
    @PreAuthorize("@ss.hasPermi('patient:card:cancel')")
    @OperationLog(module = "患者管理", operation = "注销就诊卡")
    @PutMapping("/card/{id}/cancel")
    public Result<Void> cancelCard(@PathVariable Long id,
                                   @RequestParam(required = false) String reason) {
        patientService.cancelCard(id, reason);
        return Result.success();
    }

    /**
     * 随访分页。
     */
    @Operation(summary = "随访分页")
    @PreAuthorize("@ss.hasPermi('patient:followup:list')")
    @GetMapping("/followup/page")
    public Result<PageResult<BizFollowUp>> followUpPage(PageQuery query,
                                                         @RequestParam(required = false) Long patientId,
                                                         @RequestParam(required = false) Integer status) {
        return Result.success(patientService.pageFollowUp(query, patientId, status));
    }

    /**
     * 新增随访。
     */
    @Operation(summary = "新增随访")
    @PreAuthorize("@ss.hasPermi('patient:followup:add')")
    @OperationLog(module = "患者管理", operation = "新增随访")
    @PostMapping("/followup")
    public Result<BizFollowUp> saveFollowUp(@RequestBody BizFollowUp followUp) {
        return Result.success(patientService.saveFollowUp(followUp));
    }

    /**
     * 待办随访提醒。
     */
    @Operation(summary = "待办随访")
    @PreAuthorize("@ss.hasPermi('patient:followup:list')")
    @GetMapping("/followup/pending")
    public Result<List<BizFollowUp>> pendingFollowUp() {
        return Result.success(patientService.pendingFollowUps());
    }

    /**
     * 患者档案汇总，供详情页一次性拉取。
     */
    @Operation(summary = "患者档案汇总")
    @PreAuthorize("@ss.hasPermi('patient:list')")
    @GetMapping("/{id}/summary")
    public Result<Map<String, Object>> summary(@PathVariable Long id) {
        PatientVO patient = patientService.detail(id);
        return Result.success(Map.of(
                "patient", patient,
                "records", patientService.listRecords(id),
                "cards", patientService.listCards(id)));
    }
}
