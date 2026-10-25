package com.xunfang.healthcare.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xunfang.healthcare.mapper.ReportMapper;
import com.xunfang.healthcare.entity.SysReportConfig;
import com.xunfang.healthcare.mapper.SysReportConfigMapper;
import com.xunfang.healthcare.util.SimplePdfWriter;
import com.xunfang.healthcare.vo.StatItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计报表：门诊、住院、收入、药品、患者五大主题。
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReportMapper reportMapper;
    private final ChargeService chargeService;
    private final SysReportConfigMapper reportConfigMapper;

    /**
     * 自定义报表配置列表。
     */
    public List<SysReportConfig> listCustomReports() {
        return reportConfigMapper.selectList(Wrappers.<SysReportConfig>lambdaQuery()
                .orderByDesc(SysReportConfig::getId));
    }

    /**
     * 保存自定义报表配置。
     */
    public SysReportConfig saveCustomReport(SysReportConfig config) {
        if (config.getId() == null) {
            reportConfigMapper.insert(config);
        } else {
            reportConfigMapper.updateById(config);
        }
        return config;
    }

    /**
     * 执行自定义报表。
     *
     * <p>出于安全考虑不执行配置中的任意 SQL，而是把 reportType 映射到内置的统计口径，
     * 避免 SQL 注入与越权访问数据库。</p>
     */
    public Map<String, Object> executeCustomReport(Long reportId, String startDate, String endDate) {
        SysReportConfig config = reportConfigMapper.selectById(reportId);
        if (config == null) {
            throw new com.xunfang.healthcare.common.BizException("报表配置不存在");
        }
        String type = config.getReportType() == null ? "" : config.getReportType().trim();
        return switch (type) {
            case "inpatient" -> inpatientReport(startDate, endDate);
            case "income" -> incomeReport(startDate, endDate);
            case "drug" -> drugReport(startDate, endDate);
            case "patient" -> patientReport(startDate, endDate);
            case "visit" -> visitReport(startDate, endDate);
            default -> outpatientReport(startDate, endDate);
        };
    }

    /**
     * 门诊统计：按日、按科室、按医生。
     */
    public Map<String, Object> outpatientReport(String startDate, String endDate) {
        String start = startText(startDate);
        String end = endText(endDate);
        List<Map<String, Object>> daily = reportMapper.selectOutpatientDaily(start, end);
        List<Map<String, Object>> dept = reportMapper.selectOutpatientByDept(start, end);
        List<Map<String, Object>> doctor = reportMapper.selectOutpatientByDoctor(start, end, 10);

        Map<String, Object> data = new HashMap<>(8);
        data.put("daily", toStatList(daily, "stat_date", "total_count"));
        data.put("dept", toStatList(dept, "dept_name", "total_count"));
        data.put("doctor", toStatList(doctor, "doctor_name", "total_count"));
        data.put("total", sumCount(daily));
        data.put("startDate", start);
        data.put("endDate", end);
        return data;
    }

    /**
     * 住院统计：按日入出院与当前在院人数。
     */
    public Map<String, Object> inpatientReport(String startDate, String endDate) {
        String start = startText(startDate);
        String end = endText(endDate);
        List<Map<String, Object>> daily = reportMapper.selectInpatientDaily(start, end);
        List<Map<String, Object>> dept = reportMapper.selectInpatientByDept();

        List<Map<String, Object>> admission = new ArrayList<>();
        List<Map<String, Object>> discharge = new ArrayList<>();
        long admissionTotal = 0L;
        long dischargeTotal = 0L;
        for (Map<String, Object> row : daily) {
            String date = String.valueOf(row.get("stat_date"));
            long inCount = toLong(row.get("admission_count"));
            long outCount = toLong(row.get("discharge_count"));
            admissionTotal += inCount;
            dischargeTotal += outCount;
            admission.add(stat(date, inCount));
            discharge.add(stat(date, outCount));
        }

        Map<String, Object> data = new HashMap<>(8);
        data.put("admission", admission);
        data.put("discharge", discharge);
        data.put("dept", toStatList(dept, "dept_name", "in_hospital_count"));
        data.put("admissionTotal", admissionTotal);
        data.put("dischargeTotal", dischargeTotal);
        data.put("inHospitalTotal", sumCount(dept));
        data.put("startDate", start);
        data.put("endDate", end);
        return data;
    }

    /**
     * 收入统计：复用收费汇总口径。
     */
    public Map<String, Object> incomeReport(String startDate, String endDate) {
        var summary = chargeService.summary(startDate, endDate);
        Map<String, Object> data = new HashMap<>(8);
        data.put("startDate", summary.getStartDate());
        data.put("endDate", summary.getEndDate());
        data.put("totalAmount", summary.getTotalAmount());
        data.put("totalCount", summary.getTotalCount());
        data.put("daily", toStatList(summary.getDailyList()));
        data.put("payType", toStatList(summary.getPayTypeList()));
        data.put("dept", toStatList(summary.getDeptList()));
        return data;
    }

    /**
     * 药品统计：销量排行。
     */
    public Map<String, Object> drugReport(String startDate, String endDate) {
        String start = startText(startDate);
        String end = endText(endDate);
        List<Map<String, Object>> ranking = reportMapper.selectDrugRanking(start, end, 20);

        List<Map<String, Object>> quantityList = new ArrayList<>();
        List<Map<String, Object>> amountList = new ArrayList<>();
        for (Map<String, Object> row : ranking) {
            String name = String.valueOf(row.get("drug_name"));
            quantityList.add(stat(name, toLong(row.get("total_quantity"))));
            amountList.add(stat(name, toDecimal(row.get("total_amount"))));
        }

        Map<String, Object> data = new HashMap<>(8);
        data.put("ranking", ranking);
        data.put("quantity", quantityList);
        data.put("amount", amountList);
        data.put("startDate", start);
        data.put("endDate", end);
        return data;
    }

    /**
     * 患者统计：新增趋势、性别与年龄分布。
     */
    public Map<String, Object> patientReport(String startDate, String endDate) {
        String start = startText(startDate);
        String end = endText(endDate);
        List<Map<String, Object>> daily = reportMapper.selectPatientDaily(start, end);

        Map<String, Object> data = new HashMap<>(8);
        data.put("daily", toStatList(daily, "stat_date", "total_count"));
        data.put("gender", toStatList(reportMapper.selectPatientByGender(), "gender_name", "total_count"));
        data.put("ageRange", toStatList(reportMapper.selectPatientByAgeRange(), "age_range", "total_count"));
        data.put("total", sumCount(daily));
        data.put("startDate", start);
        data.put("endDate", end);
        return data;
    }

    /**
     * 就诊人次统计（跨门诊与住院）。
     */
    public Map<String, Object> visitReport(String startDate, String endDate) {
        String start = startText(startDate);
        String end = endText(endDate);
        List<Map<String, Object>> dept = reportMapper.selectVisitByDept(start, end);
        Map<String, Object> data = new HashMap<>(4);
        data.put("dept", toStatList(dept, "dept_name", "visit_count"));
        data.put("total", sumCount(dept));
        data.put("startDate", start);
        data.put("endDate", end);
        return data;
    }

    /**
     * 导出 Excel。type 取值：outpatient / inpatient / income / drug / patient。
     */
    public byte[] exportExcel(String type, String startDate, String endDate) {
        ExportTable table = buildTable(type, startDate, endDate);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<List<String>> head = new ArrayList<>();
        for (String column : table.headers()) {
            head.add(List.of(column));
        }
        EasyExcel.write(out).head(head).sheet(table.title()).doWrite(table.rows());
        return out.toByteArray();
    }

    /**
     * 导出 PDF。
     */
    public byte[] exportPdf(String type, String startDate, String endDate) {
        ExportTable table = buildTable(type, startDate, endDate);
        return SimplePdfWriter.write(table.title(), table.headers(), table.rows());
    }

    /**
     * 构造导出表格。
     */
    private ExportTable buildTable(String type, String startDate, String endDate) {
        String normalized = StrUtil.isBlank(type) ? "outpatient" : type.trim();
        return switch (normalized) {
            case "inpatient" -> inpatientTable(startDate, endDate);
            case "income" -> incomeTable(startDate, endDate);
            case "drug" -> drugTable(startDate, endDate);
            case "patient" -> patientTable(startDate, endDate);
            default -> outpatientTable(startDate, endDate);
        };
    }

    private ExportTable outpatientTable(String startDate, String endDate) {
        String start = startText(startDate);
        String end = endText(endDate);
        List<Map<String, Object>> daily = reportMapper.selectOutpatientDaily(start, end);
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> row : daily) {
            rows.add(List.of(text(row.get("stat_date")), text(row.get("total_count")),
                    text(row.get("expert_count"))));
        }
        return new ExportTable("Outpatient Report", List.of("Date", "Total", "Expert"), rows);
    }

    private ExportTable inpatientTable(String startDate, String endDate) {
        String start = startText(startDate);
        String end = endText(endDate);
        List<Map<String, Object>> daily = reportMapper.selectInpatientDaily(start, end);
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> row : daily) {
            rows.add(List.of(text(row.get("stat_date")), text(row.get("admission_count")),
                    text(row.get("discharge_count"))));
        }
        return new ExportTable("Inpatient Report", List.of("Date", "Admission", "Discharge"), rows);
    }

    private ExportTable incomeTable(String startDate, String endDate) {
        var summary = chargeService.summary(startDate, endDate);
        List<List<String>> rows = new ArrayList<>();
        for (StatItemVO item : summary.getDailyList()) {
            rows.add(List.of(item.getName(), text(item.getValue())));
        }
        return new ExportTable("Income Report", List.of("Date", "Amount"), rows);
    }

    private ExportTable drugTable(String startDate, String endDate) {
        String start = startText(startDate);
        String end = endText(endDate);
        List<Map<String, Object>> ranking = reportMapper.selectDrugRanking(start, end, 20);
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> row : ranking) {
            rows.add(List.of(text(row.get("drug_name")), text(row.get("total_quantity")),
                    text(row.get("total_amount"))));
        }
        return new ExportTable("Drug Report", List.of("Drug", "Quantity", "Amount"), rows);
    }

    private ExportTable patientTable(String startDate, String endDate) {
        String start = startText(startDate);
        String end = endText(endDate);
        List<Map<String, Object>> daily = reportMapper.selectPatientDaily(start, end);
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> row : daily) {
            rows.add(List.of(text(row.get("stat_date")), text(row.get("total_count"))));
        }
        return new ExportTable("Patient Report", List.of("Date", "New Patients"), rows);
    }

    /**
     * 把查询结果转换为 name / value 结构。
     */
    private List<Map<String, Object>> toStatList(List<Map<String, Object>> rows, String nameKey, String valueKey) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            list.add(stat(text(row.get(nameKey)), row.get(valueKey)));
        }
        return list;
    }

    /**
     * 把统计项转换为 name / value 结构。
     */
    private List<Map<String, Object>> toStatList(List<StatItemVO> items) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (StatItemVO item : items) {
            list.add(stat(item.getName(), item.getValue()));
        }
        return list;
    }

    private Map<String, Object> stat(String name, Object value) {
        Map<String, Object> item = new HashMap<>(4);
        item.put("name", name);
        item.put("value", value);
        return item;
    }

    private long sumCount(List<Map<String, Object>> rows) {
        long total = 0L;
        for (Map<String, Object> row : rows) {
            total += toLong(row.get("total_count"));
        }
        return total;
    }

    private String startText(String startDate) {
        return parseDay(startDate, LocalDate.now().withDayOfMonth(1)).format(DAY);
    }

    private String endText(String endDate) {
        return parseDay(endDate, LocalDate.now()).format(DAY) + " 23:59:59";
    }

    private LocalDate parseDay(String text, LocalDate defaultValue) {
        if (StrUtil.isBlank(text)) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(text, DAY);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 导出用表格结构。
     */
    private record ExportTable(String title, List<String> headers, List<List<String>> rows) {
    }
}
