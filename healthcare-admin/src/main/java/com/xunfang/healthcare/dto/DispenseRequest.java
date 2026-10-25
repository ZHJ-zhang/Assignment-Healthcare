package com.xunfang.healthcare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发药 / 退药请求。
 */
@Data
@Schema(description = "发药退药请求")
public class DispenseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "处方不能为空")
    @Schema(description = "处方 ID")
    private Long prescriptionId;

    @Schema(description = "库房 ID")
    private Long warehouseId;

    @Schema(description = "需要发药的处方明细 ID，为空表示全部明细")
    private List<Long> itemIds;

    @Schema(description = "退药原因")
    private String reason;
}
