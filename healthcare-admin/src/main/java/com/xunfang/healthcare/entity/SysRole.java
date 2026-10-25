package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称。
     */
    private String roleName;

    /**
     * 角色编码，例如 admin、doctor。
     */
    private String roleCode;

    /**
     * 排序。
     */
    private Integer sort;

    /**
     * 数据权限范围：1全部 2本部门 3本部门及以下 4仅本人 5自定义。
     */
    private Integer dataScope;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 状态 0禁用1启用。
     */
    private Integer status;
}
