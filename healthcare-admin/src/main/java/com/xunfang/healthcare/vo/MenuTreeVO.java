package com.xunfang.healthcare.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点，用于菜单管理与角色授权。
 */
@Data
@Schema(description = "菜单树节点")
public class MenuTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String menuName;

    /**
     * 菜单类型：M目录 C菜单 F按钮。
     */
    private String menuType;

    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private List<MenuTreeVO> children = new ArrayList<>();
}
