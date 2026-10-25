package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典类型表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 字典名称。
     */
    private String dictName;

    /**
     * 字典类型，例如 sys_gender。
     */
    private String dictType;

    /**
     * 状态 0禁用1启用。
     */
    private Integer status;

    /**
     * 备注。
     */
    private String remark;
}
