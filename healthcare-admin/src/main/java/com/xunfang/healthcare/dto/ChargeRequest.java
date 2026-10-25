package com.xunfang.healthcare.dto;

import com.xunfang.healthcare.entity.BizChargeItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 收费请求：收费单主体 + 收费明细。
 */
@Data
@Schema(description = "收费请求")
public class ChargeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "患者不能为空")
    @Schema(description = "患者 ID")
    private Long patientId;

    @Schema(description = "科室 ID")
    private Long deptId;

    @Schema(description = "就诊 ID（挂号记录 ID）；不传时按患者当前在诊挂号自动带出")
    private Long visitId;

    @Schema(description = "处方 ID")
    private Long prescriptionId;

    @Schema(description = "待缴费的检查申请单 ID 列表，收费时自动生成检查明细并推进申请单状态")
    private List<Long> examApplyIds = new ArrayList<>();

    @Schema(description = "收费类型 1挂号2门诊3住院")
    private Integer visitType = 2;

    @Schema(description = "支付方式 1现金2微信3支付宝4医保5银行卡")
    private Integer payType = 1;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Valid
    @Schema(description = "收费明细")
    private List<BizChargeItem> items = new ArrayList<>();
}
