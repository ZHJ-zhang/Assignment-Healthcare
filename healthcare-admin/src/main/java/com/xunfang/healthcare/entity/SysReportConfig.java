package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义报表配置表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_report_config")
public class SysReportConfig extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 报表名称。
     */
    private String reportName;

    /**
     * 报表类型。
     */
    private String reportType;

    /**
     * 查询 SQL，仅允许执行只读查询语句。
     */
    private String querySql;

    /**
     * 参数配置（JSON）。
     */
    private String params;

    /**
     * 创建人。
     */
    private Long creatorId;
}
