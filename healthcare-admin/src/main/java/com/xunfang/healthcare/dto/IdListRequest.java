package com.xunfang.healthcare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用 ID 列表请求，用于角色授权等场景。
 */
@Data
@Schema(description = "ID 列表请求")
public class IdListRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID 列表")
    private List<Long> ids = new ArrayList<>();
}
