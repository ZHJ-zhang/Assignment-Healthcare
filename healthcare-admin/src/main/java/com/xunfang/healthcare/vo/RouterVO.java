package com.xunfang.healthcare.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 前端动态路由。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "动态路由")
public class RouterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "路由名称")
    private String name;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "重定向地址")
    private String redirect;

    @Schema(description = "路由元信息")
    private MetaVO meta;

    @Schema(description = "子路由")
    private List<RouterVO> children;

    /**
     * 路由元信息。
     */
    @Data
    public static class MetaVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "是否隐藏")
        private Boolean hidden;

        @Schema(description = "是否缓存")
        private Boolean keepAlive;
    }
}
