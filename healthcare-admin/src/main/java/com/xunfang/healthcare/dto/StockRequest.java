package com.xunfang.healthcare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 药品出入库请求。
 */
@Data
@Schema(description = "药品出入库请求")
public class StockRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "药品不能为空")
    @Schema(description = "药品 ID")
    private Long drugId;

    @Schema(description = "批号")
    private String batchNo;

    @NotNull(message = "数量不能为空")
    @Schema(description = "数量，出库时为正数")
    private Integer quantity;

    @Schema(description = "库房 ID")
    private Long warehouseId;

    @Schema(description = "生产日期")
    private LocalDate produceDate;

    @Schema(description = "有效期")
    private LocalDate expireDate;

    @Schema(description = "预警数量")
    private Integer warnNum;

    @Schema(description = "关联单据号")
    private String bizNo;

    @Schema(description = "备注")
    private String remark;
}
