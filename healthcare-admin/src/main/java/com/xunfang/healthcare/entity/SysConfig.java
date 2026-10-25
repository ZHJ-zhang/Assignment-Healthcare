package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 参数名称。
     */
    private String configName;

    /**
     * 参数键。
     */
    private String configKey;

    /**
     * 参数值。
     */
    private String configValue;

    /**
     * 是否内置 0否1是。
     */
    private Integer builtIn;

    /**
     * 备注。
     */
    private String remark;
}
