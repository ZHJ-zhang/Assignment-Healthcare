package com.xunfang.healthcare.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求参数，与文档 1.1 节一致。
 */
@Data
@Schema(description = "分页请求参数")
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码，从1开始", example = "1")
    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认10", example = "10")
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 500, message = "每页条数最大为 500")
    private Integer pageSize = 10;

    @Schema(description = "模糊搜索关键字")
    private String keyword;

    @Schema(description = "开始时间 yyyy-MM-dd")
    private String startTime;

    @Schema(description = "结束时间 yyyy-MM-dd")
    private String endTime;
}
