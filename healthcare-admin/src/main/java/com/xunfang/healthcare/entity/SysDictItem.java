package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典项表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class SysDictItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 字典类型。
     */
    private String dictType;

    /**
     * 字典标签。
     */
    private String dictLabel;

    /**
     * 字典键值。
     */
    private String dictValue;

    /**
     * 排序。
     */
    private Integer sort;

    /**
     * 样式：default/primary/success/warning/danger。
     */
    private String cssClass;

    /**
     * 是否默认 0否1是。
     */
    private Integer isDefault;

    /**
     * 状态 0禁用1启用。
     */
    private Integer status;

    /**
     * 备注。
     */
    private String remark;
}
