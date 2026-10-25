package com.xunfang.healthcare.dto;

import com.xunfang.healthcare.entity.BizPrescriptionItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 开具处方请求：处方主体 + 药品明细。
 */
@Data
@Schema(description = "开具处方请求")
public class PrescriptionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "患者不能为空")
    @Schema(description = "患者 ID")
    private Long patientId;

    @Schema(description = "就诊 ID")
    private Long visitId;

    @Schema(description = "科室 ID，默认取当前用户科室")
    private Long deptId;

    @Schema(description = "处方类型 1西药2中药3中成药")
    private Integer prescType = 1;

    @Schema(description = "备注")
    private String remark;

    @Valid
    @Schema(description = "处方明细")
    private List<BizPrescriptionItem> items = new ArrayList<>();
}
