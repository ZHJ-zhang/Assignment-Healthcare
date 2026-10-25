package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据备份表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_backup")
public class SysBackup extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 备份名称。
     */
    private String backupName;

    /**
     * 备份路径。
     */
    private String backupPath;

    /**
     * 文件大小（字节）。
     */
    private Long backupSize;

    /**
     * 备份类型 1手动2自动。
     */
    private Integer backupType;

    /**
     * 状态 0失败1成功。
     */
    private Integer status;
}
