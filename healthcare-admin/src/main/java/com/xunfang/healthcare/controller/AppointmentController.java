package com.xunfang.healthcare.controller;

import com.xunfang.healthcare.annotation.OperationLog;
import com.xunfang.healthcare.common.PageQuery;
import com.xunfang.healthcare.common.PageResult;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizAppointment;
import com.xunfang.healthcare.entity.BizRegistration;
import com.xunfang.healthcare.service.AppointmentService;
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

/**
 * 预约挂号。
 */
@Tag(name = "预约挂号")
@RestController
@RequestMapping("/api/appointment")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * 预约分页。
     */
    @Operation(summary = "预约分页")
    @PreAuthorize("@ss.hasPermi('appointment:list')")
    @GetMapping("/page")
    public Result<PageResult<BizAppointment>> page(PageQuery query,
                                                   @RequestParam(required = false) Long deptId,
                                                   @RequestParam(required = false) Integer status) {
        return Result.success(appointmentService.page(query, deptId, status));
    }

    /**
     * 新增预约。
     */
    @Operation(summary = "新增预约")
    @PreAuthorize("@ss.hasPermi('appointment:add')")
    @OperationLog(module = "挂号预约", operation = "新增预约")
    @PostMapping
    public Result<BizAppointment> create(@RequestBody BizAppointment appointment) {
        return Result.success(appointmentService.create(appointment));
    }

    /**
     * 取消预约。
     */
    @Operation(summary = "取消预约")
    @PreAuthorize("@ss.hasPermi('appointment:cancel')")
    @OperationLog(module = "挂号预约", operation = "取消预约")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        appointmentService.cancel(id, reason);
        return Result.success();
    }

    /**
     * 爽约登记。
     */
    @Operation(summary = "爽约登记")
    @PreAuthorize("@ss.hasPermi('appointment:cancel')")
    @OperationLog(module = "挂号预约", operation = "预约爽约")
    @PutMapping("/{id}/noshow")
    public Result<Void> noShow(@PathVariable Long id) {
        appointmentService.noShow(id);
        return Result.success();
    }

    /**
     * 改约。
     */
    @Operation(summary = "改约")
    @PreAuthorize("@ss.hasPermi('appointment:add')")
    @OperationLog(module = "挂号预约", operation = "预约改约")
    @PutMapping("/{id}/reschedule")
    public Result<BizAppointment> reschedule(@PathVariable Long id,
                                              @RequestParam Long scheduleId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                              LocalDate appointmentDate) {
        return Result.success(appointmentService.reschedule(id, scheduleId, appointmentDate));
    }

    /**
     * 取号：线上预约转正式挂号。
     * <p>
     * 不额外扣号源——预约成功时那个号已经占掉了，这里只是把它落成挂号记录并分配排队号。
     * 权限沿用挂号的新增权限，因为取号本质上就是一次挂号。
     */
    @Operation(summary = "取号（预约转挂号）")
    @PreAuthorize("@ss.hasPermi('registration:add')")
    @OperationLog(module = "挂号预约", operation = "预约取号")
    @PostMapping("/{id}/checkin")
    public Result<BizRegistration> checkin(@PathVariable Long id,
                                            @RequestParam(required = false) Integer payType) {
        return Result.success(appointmentService.checkin(id, payType));
    }
}
