package com.xunfang.healthcare.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xunfang.healthcare.common.Result;
import com.xunfang.healthcare.entity.BizAdmission;
import com.xunfang.healthcare.entity.BizPatient;
import com.xunfang.healthcare.entity.BizRegistration;
import com.xunfang.healthcare.mapper.BizAdmissionMapper;
import com.xunfang.healthcare.mapper.BizPatientMapper;
import com.xunfang.healthcare.mapper.BizRegistrationMapper;
import com.xunfang.healthcare.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页看板：核心指标与近期趋势。
 */
@Tag(name = "首页看板")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final BizPatientMapper patientMapper;
    private final BizRegistrationMapper registrationMapper;
    private final BizAdmissionMapper admissionMapper;
    private final ReportService reportService;

    /**
     * 首页汇总指标。
     */
    @Operation(summary = "首页汇总指标")
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        String today = LocalDate.now().toString();
        long patientTotal = patientMapper.selectCount(null);
        long patientToday = patientMapper.selectCount(Wrappers.<BizPatient>lambdaQuery()
                .ge(BizPatient::getCreateTime, today));
        long registrationToday = registrationMapper.selectCount(Wrappers.<BizRegistration>lambdaQuery()
                .ge(BizRegistration::getRegTime, today));
        long inHospital = admissionMapper.selectCount(Wrappers.<BizAdmission>lambdaQuery()
                .eq(BizAdmission::getStatus, 0));

        Map<String, Object> data = new HashMap<>(8);
        data.put("patientTotal", patientTotal);
        data.put("patientToday", patientToday);
        data.put("registrationToday", registrationToday);
        data.put("inHospital", inHospital);
        data.put("outpatient", reportService.outpatientReport(null, null));
        data.put("income", reportService.incomeReport(null, null));
        return Result.success(data);
    }

    /**
     * 近期门诊趋势，供首页折线图使用。
     */
    @Operation(summary = "首页门诊趋势")
    @GetMapping("/trend")
    @SuppressWarnings("unchecked")
    public Result<List<Map<String, Object>>> trend() {
        Object daily = reportService.outpatientReport(null, null).get("daily");
        return Result.success(daily instanceof List ? (List<Map<String, Object>>) daily : List.of());
    }
}
