package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizCallRecord;
import com.xunfang.healthcare.entity.BizRegistration;
import com.xunfang.healthcare.entity.BizSchedule;
import com.xunfang.healthcare.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班、挂号、退号与候诊叫号。
 */
@Tag(name = "挂号预约")
@RestController
@RequestMapping("/api/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * 排班查询。
     */
    @Operation(summary = "排班查询")
    @PreAuthorize("@ss.hasPermi('registration:schedule:list')")
    @GetMapping("/schedule")
    public Result<List<BizSchedule>> schedules(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long doctorId) {
        return Result.success(registrationService.listSchedules(date, deptId, doctorId));
    }

    /**
     * 新增排班。
     */
    @Operation(summary = "新增排班")
    @PreAuthorize("@ss.hasPermi('registration:schedule:add')")
    @OperationLog(module = "挂号预约", operation = "新增排班")
    @PostMapping("/schedule")
    public Result<BizSchedule> saveSchedule(@RequestBody BizSchedule schedule) {
        return Result.success(registrationService.saveSchedule(schedule));
    }

    /**
     * 修改排班。
     */
    @Operation(summary = "修改排班")
    @PreAuthorize("@ss.hasPermi('registration:schedule:edit')")
    @OperationLog(module = "挂号预约", operation = "修改排班")
    @PutMapping("/schedule/{id}")
    public Result<BizSchedule> updateSchedule(@PathVariable Long id, @RequestBody BizSchedule schedule) {
        schedule.setId(id);
        return Result.success(registrationService.saveSchedule(schedule));
    }

    /**
     * 停诊。
     */
    @Operation(summary = "停诊")
    @PreAuthorize("@ss.hasPermi('registration:schedule:stop')")
    @OperationLog(module = "挂号预约", operation = "排班停诊")
    @PutMapping("/schedule/{id}/stop")
    public Result<Void> stopSchedule(@PathVariable Long id) {
        registrationService.stopSchedule(id);
        return Result.success();
    }

    /**
     * 挂号。
     */
    @Operation(summary = "挂号")
    @PreAuthorize("@ss.hasPermi('registration:add')")
    @OperationLog(module = "挂号预约", operation = "挂号")
    @PostMapping
    public Result<BizRegistration> register(@RequestParam Long patientId,
                                             @RequestParam Long scheduleId,
                                             @RequestParam(required = false) Integer regType,
                                             @RequestParam(required = false) Integer payType) {
        return Result.success(registrationService.register(patientId, scheduleId, regType, payType));
    }

    /**
     * 退号。
     */
    @Operation(summary = "退号")
    @PreAuthorize("@ss.hasPermi('registration:cancel')")
    @OperationLog(module = "挂号预约", operation = "退号")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        registrationService.cancel(id, reason);
        return Result.success();
    }

    /**
     * 候诊队列。
     */
    @Operation(summary = "候诊队列")
    @PreAuthorize("@ss.hasPermi('registration:queue:list')")
    @GetMapping("/queue/{deptId}")
    public Result<List<BizRegistration>> queue(@PathVariable Long deptId,
                                               @RequestParam(required = false) Long doctorId) {
        return Result.success(registrationService.queue(deptId, doctorId));
    }

    /**
     * 候诊队列（不指定科室＝全部科室）。
     * <p>大屏默认走这个：原来前端硬取科室列表第一条当默认值，
     * 一旦有 sort 更小的科室（哪怕是空科室），屏上就一条号都没有。</p>
     */
    @Operation(summary = "候诊队列（全部科室）")
    @PreAuthorize("@ss.hasPermi('registration:queue:list')")
    @GetMapping("/queue")
    public Result<List<BizRegistration>> queueAll(@RequestParam(required = false) Long deptId,
                                                  @RequestParam(required = false) Long doctorId) {
        return Result.success(registrationService.queue(deptId, doctorId));
    }

    /**
     * 叫号。
     */
    @Operation(summary = "叫号")
    @PreAuthorize("@ss.hasPermi('registration:call')")
    @OperationLog(module = "挂号预约", operation = "叫号")
    @PostMapping("/call/{id}")
    public Result<BizCallRecord> call(@PathVariable Long id) {
        return Result.success(registrationService.call(id));
    }

    /**
     * 过号：患者被叫到但没来，退回候诊队列可重新叫号。
     */
    @Operation(summary = "过号")
    @PreAuthorize("@ss.hasPermi('registration:call')")
    @OperationLog(module = "挂号预约", operation = "叫号过号")
    @PutMapping("/call/{id}/pass")
    public Result<Void> passCall(@PathVariable Long id) {
        registrationService.passCall(id);
        return Result.success();
    }

    /**
     * 叫号后完成就诊。
     */
    @Operation(summary = "叫号完成就诊")
    @PreAuthorize("@ss.hasPermi('registration:call')")
    @OperationLog(module = "挂号预约", operation = "叫号完成就诊")
    @PutMapping("/call/{id}/finish")
    public Result<Void> finishCall(@PathVariable Long id) {
        registrationService.finishCall(id);
        return Result.success();
    }

    /**
     * 挂号记录分页。
     */
    @Operation(summary = "挂号记录分页")
    @PreAuthorize("@ss.hasPermi('registration:list')")
    @GetMapping("/page")
    public Result<PageResult<BizRegistration>> page(PageQuery query,
                                                    @RequestParam(required = false) Long deptId,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Long doctorId,
                                                    @RequestParam(required = false) Long patientId) {
        return Result.success(registrationService.pageRegistrations(query, deptId, status, doctorId, patientId));
    }

    /**
     * 接诊。
     */
    @Operation(summary = "接诊")
    @PreAuthorize("@ss.hasPermi('clinic:visit:receive')")
    @OperationLog(module = "挂号预约", operation = "接诊")
    @PostMapping("/{id}/receive")
    public Result<BizRegistration> receive(@PathVariable Long id) {
        return Result.success(registrationService.receive(id));
    }

    /**
     * 完成就诊。
     */
    @Operation(summary = "完成就诊")
    @PreAuthorize("@ss.hasPermi('clinic:visit:receive')")
    @OperationLog(module = "挂号预约", operation = "完成就诊")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        registrationService.complete(id);
        return Result.success();
    }
}
