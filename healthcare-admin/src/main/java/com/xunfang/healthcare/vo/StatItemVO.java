package com.xunfang.healthcare.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 图表数据项：名称 + 数值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "图表数据项")
public class StatItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "名称 / 分类")
    private String name;

    @Schema(description = "数值")
    private BigDecimal value;

    public StatItemVO(String name, Number value) {
        this.name = name;
        this.value = value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
