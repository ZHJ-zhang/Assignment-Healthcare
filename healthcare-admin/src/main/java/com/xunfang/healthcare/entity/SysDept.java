package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 科室表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 父级 ID。
     */
    private Long parentId;

    /**
     * 科室名称。
     */
    private String deptName;

    /**
     * 科室编码。
     */
    private String deptCode;

    /**
     * 类型 1门诊2住院3医技4行政。
     */
    private Integer deptType;

    /**
     * 联系电话。
     */
    private String phone;

    /**
     * 排序。
     */
    private Integer sort;

    /**
     * 负责人。
     */
    private String leader;

    /**
     * 状态 0禁用1启用。
     */
    private Integer status;

    /**
     * 子科室（非数据库字段）。
     */
    @TableField(exist = false)
    private List<SysDept> children = new ArrayList<>();
}
