package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单与权限表，同时承载菜单权限与按钮权限。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单名称。
     */
    private String menuName;

    /**
     * 父级菜单 ID，顶级为 0。
     */
    private Long parentId;

    /**
     * 菜单类型：M目录 C菜单 F按钮。
     */
    private String menuType;

    /**
     * 前端路由地址。
     */
    private String path;

    /**
     * 前端组件路径。
     */
    private String component;

    /**
     * 路由参数。
     */
    private String queryParam;

    /**
     * 是否外链。
     */
    private Integer isFrame;

    /**
     * 是否缓存页面。
     */
    private Integer isCache;

    /**
     * 权限标识，例如 system:user:list。
     */
    private String perms;

    /**
     * 图标。
     */
    private String icon;

    /**
     * 排序。
     */
    private Integer sort;

    /**
     * 是否显示 0隐藏1显示。
     */
    private Integer visible;

    /**
     * 状态 0禁用1启用。
     */
    private Integer status;
}
