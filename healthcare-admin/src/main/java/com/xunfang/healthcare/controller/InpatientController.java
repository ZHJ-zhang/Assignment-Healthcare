package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizAdmission;
import com.xunfang.healthcare.entity.BizBed;
import com.xunfang.healthcare.entity.BizCourseRecord;
import com.xunfang.healthcare.entity.BizMedicalOrder;
import com.xunfang.healthcare.entity.BizNursingRecord;
import com.xunfang.healthcare.entity.BizOrderExecution;
import com.xunfang.healthcare.entity.BizSettlement;
import com.xunfang.healthcare.service.InpatientService;
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

import java.math.BigDecimal;
import java.util.List;

/**
 * 住院管理：床位、入出院、医嘱、护理与结算。
 */
@Tag(name = "住院管理")
@RestController
@RequestMapping("/api/inpatient")
@RequiredArgsConstructor
public class InpatientController {

    private final InpatientService inpatientService;

    /**
     * 床位列表。
     */
    @Operation(summary = "床位列表")
    @PreAuthorize("@ss.hasPermi('inpatient:bed:list')")
    @GetMapping("/bed/list")
    public Result<List<BizBed>> beds(@RequestParam(required = false) Long deptId,
                                     @RequestParam(required = false) Long wardId,
                                     @RequestParam(required = false) Integer status) {
        return Result.success(inpatientService.listBeds(deptId, wardId, status));
    }

    /**
     * 新增床位。
     */
    @Operation(summary = "新增床位")
    @PreAuthorize("@ss.hasPermi('inpatient:bed:add')")
    @OperationLog(module = "住院管理", operation = "新增床位")
    @PostMapping("/bed")
    public Result<BizBed> saveBed(@RequestBody BizBed bed) {
        return Result.success(inpatientService.saveBed(bed));
    }

    /**
     * 修改床位。
     */
    @Operation(summary = "修改床位")
    @PreAuthorize("@ss.hasPermi('inpatient:bed:edit')")
    @OperationLog(module = "住院管理", operation = "修改床位")
    @PutMapping("/bed/{id}")
    public Result<BizBed> updateBed(@PathVariable Long id, @RequestBody BizBed bed) {
        bed.setId(id);
        return Result.success(inpatientService.saveBed(bed));
    }

    /**
     * 床位转维护。
     */
    @Operation(summary = "床位转维护")
    @PreAuthorize("@ss.hasPermi('inpatient:bed:edit')")
    @OperationLog(module = "住院管理", operation = "床位转维护")
    @PutMapping("/bed/{id}/maintain")
    public Result<BizBed> maintainBed(@PathVariable Long id) {
        return Result.success(inpatientService.maintainBed(id));
    }

    /**
     * 维护完成，恢复使用。
     */
    @Operation(summary = "床位恢复使用")
    @PreAuthorize("@ss.hasPermi('inpatient:bed:edit')")
    @OperationLog(module = "住院管理", operation = "床位恢复使用")
    @PutMapping("/bed/{id}/recover")
    public Result<BizBed> recoverBed(@PathVariable Long id) {
        return Result.success(inpatientService.recoverBed(id));
    }

    /**
     * 入院登记。
     */
    @Operation(summary = "入院登记")
    @PreAuthorize("@ss.hasPermi('inpatient:admission:add')")
    @OperationLog(module = "住院管理", operation = "入院登记")
    @PostMapping("/admission")
    public Result<BizAdmission> createAdmission(@RequestBody BizAdmission admission) {
        return Result.success(inpatientService.createAdmission(admission));
    }

    /**
     * 入院记录分页。
     */
    @Operation(summary = "入院记录分页")
    @PreAuthorize("@ss.hasPermi('inpatient:admission:list')")
    @GetMapping("/admission/page")
    public Result<PageResult<BizAdmission>> admissionPage(PageQuery query,
                                                          @RequestParam(required = false) Long deptId,
                                                          @RequestParam(required = false) Integer status,
                                                          @RequestParam(required = false) Long patientId) {
        return Result.success(inpatientService.pageAdmissions(query, deptId, status, patientId));
    }

    /**
     * 出院办理。
     */
    @Operation(summary = "出院办理")
    @PreAuthorize("@ss.hasPermi('inpatient:admission:discharge')")
    @OperationLog(module = "住院管理", operation = "出院办理")
    @PutMapping("/admission/{id}/discharge")
    public Result<Void> discharge(@PathVariable Long id) {
        inpatientService.discharge(id);
        return Result.success();
    }

    /**
     * 转科。
     */
    @Operation(summary = "转科")
    @PreAuthorize("@ss.hasPermi('inpatient:admission:transfer')")
    @OperationLog(module = "住院管理", operation = "转科")
    @PostMapping("/admission/{id}/transfer")
    public Result<Void> transfer(@PathVariable Long id,
                                  @RequestParam Long deptId,
                                  @RequestParam(required = false) Long bedId) {
        inpatientService.transfer(id, deptId, bedId);
        return Result.success();
    }

    /**
     * 医嘱分页。
     */
    @Operation(summary = "医嘱分页")
    @PreAuthorize("@ss.hasPermi('inpatient:order:list')")
    @GetMapping("/order/page")
    public Result<PageResult<BizMedicalOrder>> orderPage(PageQuery query,
                                                          @RequestParam(required = false) Long admissionId,
                                                          @RequestParam(required = false) Integer status) {
        return Result.success(inpatientService.pageOrders(query, admissionId, status));
    }

    /**
     * 开医嘱。
     */
    @Operation(summary = "开医嘱")
    @PreAuthorize("@ss.hasPermi('inpatient:order:add')")
    @OperationLog(module = "住院管理", operation = "开医嘱")
    @PostMapping("/order")
    public Result<BizMedicalOrder> createOrder(@RequestBody BizMedicalOrder order) {
        return Result.success(inpatientService.createOrder(order));
    }

    /**
     * 执行医嘱。
     */
    @Operation(summary = "执行医嘱")
    @PreAuthorize("@ss.hasPermi('inpatient:order:execute')")
    @OperationLog(module = "住院管理", operation = "执行医嘱")
    @PutMapping("/order/{id}/execute")
    public Result<BizOrderExecution> executeOrder(@PathVariable Long id,
                                                   @RequestParam(required = false) String result) {
        return Result.success(inpatientService.executeOrder(id, result));
    }

    /**
     * 完成医嘱。
     */
    @Operation(summary = "完成医嘱")
    @PreAuthorize("@ss.hasPermi('inpatient:order:execute')")
    @OperationLog(module = "住院管理", operation = "完成医嘱")
    @PutMapping("/order/{id}/finish")
    public Result<Void> finishOrder(@PathVariable Long id) {
        inpatientService.finishOrder(id);
        return Result.success();
    }

    /**
     * 停止医嘱。
     */
    @Operation(summary = "停止医嘱")
    @PreAuthorize("@ss.hasPermi('inpatient:order:stop')")
    @OperationLog(module = "住院管理", operation = "停止医嘱")
    @PutMapping("/order/{id}/stop")
    public Result<Void> stopOrder(@PathVariable Long id) {
        inpatientService.stopOrder(id);
        return Result.success();
    }

    /**
     * 护理记录。
     */
    @Operation(summary = "新增护理记录")
    @PreAuthorize("@ss.hasPermi('inpatient:nursing:add')")
    @OperationLog(module = "住院管理", operation = "新增护理记录")
    @PostMapping("/nursing")
    public Result<BizNursingRecord> addNursing(@RequestBody BizNursingRecord record) {
        return Result.success(inpatientService.addNursingRecord(record));
    }

    /**
     * 护理记录分页。
     */
    @Operation(summary = "护理记录分页")
    @PreAuthorize("@ss.hasPermi('inpatient:nursing:list')")
    @GetMapping("/nursing/page")
    public Result<PageResult<BizNursingRecord>> nursingPage(PageQuery query,
                                                              @RequestParam(required = false) Long admissionId,
                                                              @RequestParam(required = false) Long patientId) {
        return Result.success(inpatientService.pageNursingRecords(query, admissionId, patientId));
    }

    /**
     * 病程记录。
     */
    @Operation(summary = "新增病程记录")
    @PreAuthorize("@ss.hasPermi('inpatient:nursing:add')")
    @OperationLog(module = "住院管理", operation = "新增病程记录")
    @PostMapping("/course")
    public Result<BizCourseRecord> addCourse(@RequestBody BizCourseRecord record) {
        return Result.success(inpatientService.addCourseRecord(record));
    }

    /**
     * 病程记录分页。
     */
    @Operation(summary = "病程记录分页")
    @PreAuthorize("@ss.hasPermi('inpatient:nursing:list')")
    @GetMapping("/course/page")
    public Result<PageResult<BizCourseRecord>> coursePage(PageQuery query,
                                                            @RequestParam(required = false) Long admissionId,
                                                            @RequestParam(required = false) Long patientId) {
        return Result.success(inpatientService.pageCourseRecords(query, admissionId, patientId));
    }

    /**
     * 出院结算。
     */
    @Operation(summary = "出院结算")
    @PreAuthorize("@ss.hasPermi('inpatient:settlement:list')")
    @OperationLog(module = "住院管理", operation = "出院结算")
    @PostMapping("/settlement")
    public Result<BizSettlement> settle(@RequestParam Long admissionId,
                                         @RequestParam BigDecimal insuranceAmount,
                                         @RequestParam BigDecimal selfAmount) {
        return Result.success(inpatientService.settle(admissionId, insuranceAmount, selfAmount));
    }

    /**
     * 结算记录分页。
     */
    @Operation(summary = "结算记录分页")
    @PreAuthorize("@ss.hasPermi('inpatient:settlement:list')")
    @GetMapping("/settlement/page")
    public Result<PageResult<BizSettlement>> settlementPage(PageQuery query,
                                                            @RequestParam(required = false) Long admissionId) {
        return Result.success(inpatientService.pageSettlements(query, admissionId));
    }

    /**
     * 结算退费。
     */
    @Operation(summary = "结算退费")
    @PreAuthorize("@ss.hasPermi('inpatient:settlement:list')")
    @OperationLog(module = "住院管理", operation = "结算退费")
    @PutMapping("/settlement/{id}/refund")
    public Result<BizSettlement> refundSettlement(@PathVariable Long id,
                                                   @RequestParam String reason) {
        return Result.success(inpatientService.refundSettlement(id, reason));
    }
}
