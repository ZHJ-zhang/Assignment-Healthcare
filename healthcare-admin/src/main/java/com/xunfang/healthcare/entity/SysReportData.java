package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
/**
 * 报表数据缓存表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_report_data")
public class SysReportData extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long reportId;

    /**
     * 统计日期。
     */
    private LocalDate statDate;

    /**
     * 数据 JSON。
     */
    private String dataJson;
}
