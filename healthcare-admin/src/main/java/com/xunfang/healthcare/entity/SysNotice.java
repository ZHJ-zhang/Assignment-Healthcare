package com.xunfang.healthcare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公告表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseAuditEntity {

    private static final long serialVersionUID = 1L;

    private String title;
    private String content;

    /**
     * 公告类型 1院内2科室。
     */
    private Integer noticeType;

    /**
     * 科室 ID（可空）。
     */
    private Long deptId;

    /**
     * 发布人。
     */
    private Long publisherId;

    /**
     * 发布时间。
     */
    private LocalDateTime publishTime;

    /**
     * 状态 0草稿1已发布。
     */
    private Integer status;
}
