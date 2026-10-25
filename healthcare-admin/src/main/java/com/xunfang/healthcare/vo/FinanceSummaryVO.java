package com.xunfang.healthcare.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务汇总：总额、笔数与分组明细，供 ECharts 报表使用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "财务汇总")
public class FinanceSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "统计开始日期")
    private String startDate;

    @Schema(description = "统计结束日期")
    private String endDate;

    @Schema(description = "收入总额")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Schema(description = "收费单笔数")
    private long totalCount;

    @Schema(description = "按日汇总")
    private List<StatItemVO> dailyList = new ArrayList<>();

    @Schema(description = "按支付方式汇总")
    private List<StatItemVO> payTypeList = new ArrayList<>();

    @Schema(description = "按科室汇总")
    private List<StatItemVO> deptList = new ArrayList<>();
}
