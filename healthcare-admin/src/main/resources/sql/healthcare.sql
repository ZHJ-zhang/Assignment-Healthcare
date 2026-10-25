-- =====================================================================
--  医疗后台管理系统 数据库初始化脚本
--  MySQL 8.0 / utf8mb4
--
--  约定：
--    1. 主键 id 由应用层雪花算法生成（MyBatis-Plus assign_id），不使用自增；
--    2. 逻辑删除字段统一为 deleted（0 未删除 / 1 已删除），查询自动过滤；
--    3. 所有表带 create_time / update_time，部分表额外带 create_by / update_by；
--    4. 不建立物理外键，由业务层保证引用完整性，便于分库与批量处理；
--    5. 敏感字段（身份证号、手机号）密文存储，同时保留 _hash 摘要列用于精确检索。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `healthcare`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `healthcare`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 一、系统管理
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `username`      VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `password`      VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    `real_name`     VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    `gender`        TINYINT      DEFAULT 0 COMMENT '性别 0女1男',
    `phone`         VARCHAR(200) DEFAULT NULL COMMENT '手机号（AES 密文）',
    `phone_hash`    VARCHAR(64)  DEFAULT NULL COMMENT '手机号 SHA-256 摘要',
    `email`         VARCHAR(50)  DEFAULT NULL COMMENT '邮箱',
    `dept_id`       BIGINT       DEFAULT NULL COMMENT '所属科室ID',
    `avatar`        VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `status`        TINYINT      DEFAULT 1 COMMENT '状态 0禁用1启用',
    `remark`        VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `last_login_time` DATETIME   DEFAULT NULL COMMENT '最后登录时间',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT       DEFAULT 0 COMMENT '创建人',
    `update_by`     BIGINT       DEFAULT 0 COMMENT '更新人',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`),
    KEY `idx_sys_user_dept` (`dept_id`),
    KEY `idx_sys_user_phone_hash` (`phone_hash`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `role_name`   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    `role_code`   VARCHAR(50)  NOT NULL COMMENT '角色编码',
    `sort`        INT          DEFAULT 0 COMMENT '排序',
    `data_scope`  TINYINT      DEFAULT 2 COMMENT '数据范围 1全部2本部门及以下3本部门4仅本人',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态 0禁用1启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT       DEFAULT 0 COMMENT '创建人',
    `update_by`   BIGINT       DEFAULT 0 COMMENT '更新人',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_code` (`role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`          BIGINT   NOT NULL COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`),
    KEY `idx_sys_user_role_role` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联表';

DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `parent_id`   BIGINT       DEFAULT 0 COMMENT '父级ID',
    `dept_name`   VARCHAR(50)  NOT NULL COMMENT '科室名称',
    `dept_code`   VARCHAR(50)  DEFAULT NULL COMMENT '科室编码',
    `dept_type`   TINYINT      DEFAULT 1 COMMENT '类型 1门诊2住院3医技4行政5药房',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    `sort`        INT          DEFAULT 0 COMMENT '排序',
    `leader`      VARCHAR(50)  DEFAULT NULL COMMENT '负责人',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态 0禁用1启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_sys_dept_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '科室表';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `menu_name`   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    `parent_id`   BIGINT       DEFAULT 0 COMMENT '父级菜单ID',
    `menu_type`   CHAR(1)      DEFAULT 'C' COMMENT '类型 M目录 C菜单 F按钮',
    `path`        VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
    `component`   VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
    `query_param` VARCHAR(255) DEFAULT NULL COMMENT '路由参数',
    `is_frame`    TINYINT      DEFAULT 1 COMMENT '是否外链 0是1否',
    `is_cache`    TINYINT      DEFAULT 0 COMMENT '是否缓存 0否1是',
    `perms`       VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    `icon`        VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
    `sort`        INT          DEFAULT 0 COMMENT '排序',
    `visible`     TINYINT      DEFAULT 1 COMMENT '是否显示 0隐藏1显示',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态 0禁用1启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_sys_menu_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单权限表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `id`      BIGINT NOT NULL COMMENT '主键',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_menu` (`role_id`, `menu_id`),
    KEY `idx_sys_role_menu_menu` (`menu_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联表';

DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `dict_name`   VARCHAR(100) NOT NULL COMMENT '字典名称',
    `dict_type`   VARCHAR(100) NOT NULL COMMENT '字典类型',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态 0禁用1启用',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_dict_type` (`dict_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典类型表';

DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `dict_type`   VARCHAR(100) NOT NULL COMMENT '字典类型',
    `dict_label`  VARCHAR(100) NOT NULL COMMENT '字典标签',
    `dict_value`  VARCHAR(100) NOT NULL COMMENT '字典键值',
    `sort`        INT          DEFAULT 0 COMMENT '排序',
    `css_class`   VARCHAR(100) DEFAULT NULL COMMENT '样式属性',
    `is_default`  TINYINT      DEFAULT 0 COMMENT '是否默认 0否1是',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态 0禁用1启用',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_sys_dict_item_type` (`dict_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典数据表';

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `config_name` VARCHAR(100) NOT NULL COMMENT '参数名称',
    `config_key`  VARCHAR(100) NOT NULL COMMENT '参数键',
    `config_value` VARCHAR(500) DEFAULT NULL COMMENT '参数值',
    `built_in`    TINYINT      DEFAULT 0 COMMENT '是否内置 0否1是',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统参数表';

DROP TABLE IF EXISTS `sys_security_config`;
CREATE TABLE `sys_security_config` (
    `id`                  BIGINT      NOT NULL COMMENT '主键',
    `login_fail_limit`    TINYINT     DEFAULT 5 COMMENT '登录失败锁定次数',
    `lock_minutes`        INT         DEFAULT 30 COMMENT '锁定时长（分钟）',
    `session_timeout`     INT         DEFAULT 30 COMMENT '会话超时（分钟）',
    `ip_whitelist`        VARCHAR(500) DEFAULT NULL COMMENT 'IP 白名单',
    `two_factor_enabled`  TINYINT     DEFAULT 0 COMMENT '双因素认证 0关1开',
    `mask_enabled`        TINYINT     DEFAULT 1 COMMENT '敏感数据脱敏 0关1开',
    `password_min_length` INT         DEFAULT 6 COMMENT '密码最小长度',
    `password_expire_days` INT        DEFAULT 90 COMMENT '密码有效期（天）',
    `create_time`         DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '安全配置表';

DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
    `id`          BIGINT        NOT NULL COMMENT '主键',
    `user_id`     BIGINT        DEFAULT NULL COMMENT '操作用户ID',
    `username`    VARCHAR(50)   DEFAULT NULL COMMENT '用户名',
    `module`      VARCHAR(50)   DEFAULT NULL COMMENT '所属模块',
    `operation`   VARCHAR(100)  DEFAULT NULL COMMENT '操作描述',
    `method`      VARCHAR(200)  DEFAULT NULL COMMENT '请求方法',
    `params`      TEXT          COMMENT '请求参数',
    `ip`          VARCHAR(50)   DEFAULT NULL COMMENT 'IP地址',
    `user_agent`  VARCHAR(255)  DEFAULT NULL COMMENT '浏览器UA',
    `status`      TINYINT       DEFAULT 1 COMMENT '状态 0失败1成功',
    `cost_time`   INT           DEFAULT 0 COMMENT '耗时（毫秒）',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_sys_operation_log_user` (`user_id`),
    KEY `idx_sys_operation_log_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';

DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
    `id`         BIGINT       NOT NULL COMMENT '主键',
    `username`   VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `ip`         VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `location`   VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
    `browser`    VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
    `os`         VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
    `status`     TINYINT      DEFAULT 1 COMMENT '状态 0失败1成功',
    `msg`        VARCHAR(255) DEFAULT NULL COMMENT '提示信息',
    `login_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_sys_login_log_username` (`username`),
    KEY `idx_sys_login_log_time` (`login_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志表';

DROP TABLE IF EXISTS `sys_backup`;
CREATE TABLE `sys_backup` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `backup_name` VARCHAR(100) NOT NULL COMMENT '备份名称',
    `backup_path` VARCHAR(255) DEFAULT NULL COMMENT '备份路径',
    `backup_size` BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
    `backup_type` TINYINT      DEFAULT 1 COMMENT '类型 1手动2自动',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态 0失败1成功',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '备份时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据备份表';

DROP TABLE IF EXISTS `sys_report_config`;
CREATE TABLE `sys_report_config` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `report_name` VARCHAR(100) NOT NULL COMMENT '报表名称',
    `report_type` VARCHAR(50)  DEFAULT NULL COMMENT '报表类型',
    `query_sql`   TEXT         COMMENT '查询SQL',
    `params`      VARCHAR(500) DEFAULT NULL COMMENT '参数配置',
    `creator_id`  BIGINT       DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT       DEFAULT 0 COMMENT '创建人',
    `update_by`   BIGINT       DEFAULT 0 COMMENT '更新人',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报表配置表';

DROP TABLE IF EXISTS `sys_report_data`;
CREATE TABLE `sys_report_data` (
    `id`          BIGINT      NOT NULL COMMENT '主键',
    `report_id`   BIGINT      DEFAULT NULL COMMENT '报表ID',
    `stat_date`   DATE        DEFAULT NULL COMMENT '统计日期',
    `data_json`   TEXT        COMMENT '数据JSON',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_sys_report_data_report` (`report_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报表数据缓存表';

DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message` (
    `id`          BIGINT      NOT NULL COMMENT '主键',
    `receiver_id` BIGINT      DEFAULT NULL COMMENT '接收人ID，为空表示全体',
    `title`       VARCHAR(100) DEFAULT NULL COMMENT '标题',
    `content`     TEXT        COMMENT '内容',
    `msg_type`    TINYINT     DEFAULT 1 COMMENT '类型 1系统2待办3预警',
    `is_read`     TINYINT     DEFAULT 0 COMMENT '是否已读 0未读1已读',
    `biz_id`      VARCHAR(50) DEFAULT NULL COMMENT '来源业务单号',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_sys_message_receiver` (`receiver_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内消息表';

DROP TABLE IF EXISTS `sys_sms_record`;
CREATE TABLE `sys_sms_record` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `template_code` VARCHAR(50)  DEFAULT NULL COMMENT '模板编码',
    `content`       VARCHAR(500) DEFAULT NULL COMMENT '短信内容',
    `send_status`   TINYINT      DEFAULT 0 COMMENT '发送状态 0发送中1成功2失败',
    `error_msg`     VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    `send_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_sys_sms_record_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '短信记录表';

DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
    `id`           BIGINT       NOT NULL COMMENT '主键',
    `title`        VARCHAR(100) NOT NULL COMMENT '公告标题',
    `content`      TEXT         COMMENT '公告内容',
    `notice_type`  TINYINT      DEFAULT 1 COMMENT '类型 1院内2科室',
    `dept_id`      BIGINT       DEFAULT NULL COMMENT '科室ID',
    `publisher_id` BIGINT       DEFAULT NULL COMMENT '发布人',
    `publish_time` DATETIME     DEFAULT NULL COMMENT '发布时间',
    `status`       TINYINT      DEFAULT 0 COMMENT '状态 0草稿1已发布',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    BIGINT       DEFAULT 0 COMMENT '创建人',
    `update_by`    BIGINT       DEFAULT 0 COMMENT '更新人',
    `deleted`      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_sys_notice_dept` (`dept_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '公告表';

-- ---------------------------------------------------------------------
-- 二、患者管理
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `biz_patient`;
CREATE TABLE `biz_patient` (
    `id`               BIGINT         NOT NULL COMMENT '主键',
    `patient_no`       VARCHAR(30)   NOT NULL COMMENT '患者编号',
    `name`             VARCHAR(50)   NOT NULL COMMENT '姓名',
    `gender`           TINYINT        DEFAULT 0 COMMENT '性别 0女1男',
    `birth_date`       DATE           DEFAULT NULL COMMENT '出生日期',
    `id_card`          VARCHAR(200)  DEFAULT NULL COMMENT '身份证号（AES 密文）',
    `id_card_hash`     VARCHAR(64)   DEFAULT NULL COMMENT '身份证号 SHA-256 摘要',
    `phone`            VARCHAR(200)  DEFAULT NULL COMMENT '手机号（AES 密文）',
    `phone_hash`       VARCHAR(64)   DEFAULT NULL COMMENT '手机号 SHA-256 摘要',
    `address`          VARCHAR(255)  DEFAULT NULL COMMENT '住址',
    `blood_type`       VARCHAR(10)   DEFAULT NULL COMMENT '血型',
    `allergy_history`  VARCHAR(500)  DEFAULT NULL COMMENT '过敏史',
    `medical_history`  TEXT          COMMENT '既往病史',
    `family_history`   TEXT          COMMENT '家族病史',
    `card_no`          VARCHAR(30)   DEFAULT NULL COMMENT '就诊卡号',
    `card_balance`     DECIMAL(10, 2) DEFAULT 0.00 COMMENT '卡余额',
    `tags`             VARCHAR(255)  DEFAULT NULL COMMENT '标签（逗号分隔）',
    `dept_id`          BIGINT         DEFAULT NULL COMMENT '建档科室',
    `status`           TINYINT        DEFAULT 1 COMMENT '状态 0停用1正常',
    `create_time`      DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
    `update_time`      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`        BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`          TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_patient_no` (`patient_no`),
    KEY `idx_biz_patient_name` (`name`),
    KEY `idx_biz_patient_id_card_hash` (`id_card_hash`),
    KEY `idx_biz_patient_phone_hash` (`phone_hash`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '患者档案表';

DROP TABLE IF EXISTS `biz_patient_card`;
CREATE TABLE `biz_patient_card` (
    `id`          BIGINT         NOT NULL COMMENT '主键',
    `patient_id`  BIGINT         NOT NULL COMMENT '患者ID',
    `card_no`     VARCHAR(30)   NOT NULL COMMENT '卡号',
    `card_type`   TINYINT        DEFAULT 1 COMMENT '类型 1普通2储值3医保',
    `balance`     DECIMAL(10, 2) DEFAULT 0.00 COMMENT '卡余额',
    `status`      TINYINT        DEFAULT 1 COMMENT '状态 0挂失1正常2注销',
    `issue_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '发卡时间',
    `loss_time`   DATETIME       DEFAULT NULL COMMENT '挂失时间',
    `remark`      VARCHAR(255)   DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`   BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`     TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_patient_card_no` (`card_no`),
    KEY `idx_biz_patient_card_patient` (`patient_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '就诊卡表';

DROP TABLE IF EXISTS `biz_medical_record`;
CREATE TABLE `biz_medical_record` (
    `id`              BIGINT      NOT NULL COMMENT '主键',
    `patient_id`      BIGINT      NOT NULL COMMENT '患者ID',
    `dept_id`         BIGINT      DEFAULT NULL COMMENT '就诊科室',
    `visit_id`        BIGINT      DEFAULT NULL COMMENT '就诊ID(挂号记录ID)',
    `visit_no`        VARCHAR(30) DEFAULT NULL COMMENT '就诊流水号',
    `visit_type`      TINYINT     DEFAULT 1 COMMENT '类型 1门诊2住院',
    `chief_complaint` VARCHAR(500) DEFAULT NULL COMMENT '主诉',
    `present_illness` TEXT        COMMENT '现病史',
    `physical_exam`   TEXT        COMMENT '体格检查',
    `diagnosis`       VARCHAR(500) DEFAULT NULL COMMENT '诊断',
    `icd_code`        VARCHAR(20) DEFAULT NULL COMMENT 'ICD编码',
    `doctor_id`       BIGINT      DEFAULT NULL COMMENT '医生ID',
    `visit_time`      DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '就诊时间',
    `status`          TINYINT     DEFAULT 0 COMMENT '状态 0草稿1已提交2已归档',
    `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       BIGINT      DEFAULT 0 COMMENT '创建人',
    `update_by`       BIGINT      DEFAULT 0 COMMENT '更新人',
    `deleted`         TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_record_patient` (`patient_id`),
    KEY `idx_biz_record_dept` (`dept_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '电子病历表';

DROP TABLE IF EXISTS `biz_follow_up`;
CREATE TABLE `biz_follow_up` (
    `id`          BIGINT      NOT NULL COMMENT '主键',
    `patient_id`  BIGINT      NOT NULL COMMENT '患者ID',
    `dept_id`     BIGINT      DEFAULT NULL COMMENT '随访科室',
    `plan_date`   DATE        DEFAULT NULL COMMENT '计划随访日期',
    `follow_type` TINYINT     DEFAULT 1 COMMENT '方式 1电话2上门3短信',
    `content`     TEXT        COMMENT '随访内容',
    `result`      VARCHAR(500) DEFAULT NULL COMMENT '随访结果',
    `operator_id` BIGINT      DEFAULT NULL COMMENT '随访人',
    `status`      TINYINT     DEFAULT 0 COMMENT '状态 0待随访1已完成',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT      DEFAULT 0 COMMENT '创建人',
    `update_by`   BIGINT      DEFAULT 0 COMMENT '更新人',
    `deleted`     TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_follow_up_patient` (`patient_id`),
    KEY `idx_biz_follow_up_plan` (`plan_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '随访表';

-- ---------------------------------------------------------------------
-- 三、挂号与预约
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `biz_schedule`;
CREATE TABLE `biz_schedule` (
    `id`            BIGINT         NOT NULL COMMENT '主键',
    `doctor_id`     BIGINT         NOT NULL COMMENT '医生ID',
    `dept_id`       BIGINT         NOT NULL COMMENT '科室ID',
    `schedule_date` DATE           NOT NULL COMMENT '排班日期',
    `time_period`   TINYINT        DEFAULT 1 COMMENT '时段 1上午2下午3晚上',
    `reg_type`      TINYINT        DEFAULT 1 COMMENT '号别 1普通2专家3急诊',
    `total_num`     INT            DEFAULT 0 COMMENT '总号源',
    `remain_num`    INT            DEFAULT 0 COMMENT '剩余号源',
    `reg_fee`       DECIMAL(10, 2) DEFAULT 0.00 COMMENT '挂号费',
    `status`        TINYINT        DEFAULT 1 COMMENT '状态 0停诊1正常',
    `create_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`     BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`       TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_schedule_doctor_date` (`doctor_id`, `schedule_date`),
    KEY `idx_biz_schedule_dept_date` (`dept_id`, `schedule_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '排班表';

DROP TABLE IF EXISTS `biz_registration`;
CREATE TABLE `biz_registration` (
    `id`           BIGINT         NOT NULL COMMENT '主键',
    `reg_no`       VARCHAR(30)   NOT NULL COMMENT '挂号单号',
    `patient_id`   BIGINT         NOT NULL COMMENT '患者ID',
    `doctor_id`    BIGINT         NOT NULL COMMENT '医生ID',
    `dept_id`      BIGINT         NOT NULL COMMENT '科室ID',
    `schedule_id`  BIGINT         DEFAULT NULL COMMENT '排班ID',
    `reg_type`     TINYINT        DEFAULT 1 COMMENT '号别 1普通2专家3急诊',
    `reg_fee`      DECIMAL(10, 2) DEFAULT 0.00 COMMENT '挂号费',
    `queue_no`     INT            DEFAULT 0 COMMENT '排队号',
    `status`       TINYINT        DEFAULT 0 COMMENT '状态 0待就诊1就诊中2已完成3已退号',
    `reg_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '挂号时间',
    `cancel_reason` VARCHAR(255)  DEFAULT NULL COMMENT '退号原因',
    `create_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`    BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`      TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_registration_no` (`reg_no`),
    KEY `idx_biz_registration_patient` (`patient_id`),
    KEY `idx_biz_registration_dept_status` (`dept_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '挂号记录表';

DROP TABLE IF EXISTS `biz_appointment`;
CREATE TABLE `biz_appointment` (
    `id`               BIGINT      NOT NULL COMMENT '主键',
    `patient_id`       BIGINT      NOT NULL COMMENT '患者ID',
    `doctor_id`        BIGINT      DEFAULT NULL COMMENT '医生ID',
    `dept_id`          BIGINT      DEFAULT NULL COMMENT '科室ID',
    `schedule_id`      BIGINT      DEFAULT NULL COMMENT '排班ID',
    `registration_id`  BIGINT      DEFAULT NULL COMMENT '转挂号后的挂号记录ID',
    `appointment_date` DATE        NOT NULL COMMENT '预约日期',
    `time_period`      TINYINT     DEFAULT 1 COMMENT '时段 1上午2下午3晚上',
    `reg_type`         TINYINT     DEFAULT 1 COMMENT '号别 1普通2专家3急诊',
    `source`           TINYINT     DEFAULT 1 COMMENT '来源 1线上2线下',
    `status`           TINYINT     DEFAULT 0 COMMENT '状态 0待就诊1已取号2已取消3爽约',
    `cancel_reason`    VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    `create_time`      DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        BIGINT      DEFAULT 0 COMMENT '创建人',
    `update_by`        BIGINT      DEFAULT 0 COMMENT '更新人',
    `deleted`          TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_appointment_patient` (`patient_id`),
    KEY `idx_biz_appointment_date` (`appointment_date`),
    KEY `idx_appt_registration` (`registration_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '预约表';

DROP TABLE IF EXISTS `biz_call_record`;
CREATE TABLE `biz_call_record` (
    `id`              BIGINT   NOT NULL COMMENT '主键',
    `dept_id`         BIGINT   NOT NULL COMMENT '科室ID',
    `registration_id` BIGINT   DEFAULT NULL COMMENT '挂号ID',
    `patient_id`      BIGINT   DEFAULT NULL COMMENT '患者ID',
    `queue_no`        INT      DEFAULT 0 COMMENT '排队号',
    `call_no`         INT      DEFAULT 0 COMMENT '叫号次序',
    `call_time`       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '叫号时间',
    `status`          TINYINT  DEFAULT 1 COMMENT '状态 0已过号1就诊中2已完成',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       BIGINT   DEFAULT 0 COMMENT '创建人',
    `update_by`       BIGINT   DEFAULT 0 COMMENT '更新人',
    `deleted`         TINYINT  DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_call_record_dept` (`dept_id`),
    KEY `idx_biz_call_record_time` (`call_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '叫号记录表';

-- ---------------------------------------------------------------------
-- 四、门诊管理
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `biz_prescription`;
CREATE TABLE `biz_prescription` (
    `id`           BIGINT         NOT NULL COMMENT '主键',
    `presc_no`     VARCHAR(30)   NOT NULL COMMENT '处方号',
    `patient_id`   BIGINT         NOT NULL COMMENT '患者ID',
    `doctor_id`    BIGINT         DEFAULT NULL COMMENT '医生ID',
    `dept_id`      BIGINT         DEFAULT NULL COMMENT '科室ID',
    `visit_id`     BIGINT         DEFAULT NULL COMMENT '就诊ID(挂号记录ID)',
    `presc_type`   TINYINT        DEFAULT 1 COMMENT '类型 1西药2中药3中成药',
    `total_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '总金额',
    `status`       TINYINT        DEFAULT 0 COMMENT '状态 0待收费1已审核(历史)2已收费3已发药4已退5已作废',
    `auditor_id`   BIGINT         DEFAULT NULL COMMENT '审核人',
    `audit_time`   DATETIME       DEFAULT NULL COMMENT '审核时间',
    `remark`       VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    `create_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '开方时间',
    `update_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`    BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`      TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_prescription_no` (`presc_no`),
    KEY `idx_biz_prescription_patient` (`patient_id`),
    KEY `idx_biz_prescription_doctor` (`doctor_id`),
    KEY `idx_presc_visit` (`visit_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '处方表';

DROP TABLE IF EXISTS `biz_prescription_item`;
CREATE TABLE `biz_prescription_item` (
    `id`             BIGINT         NOT NULL COMMENT '主键',
    `prescription_id` BIGINT        NOT NULL COMMENT '处方ID',
    `drug_id`        BIGINT         DEFAULT NULL COMMENT '药品ID',
    `drug_name`      VARCHAR(100)   DEFAULT NULL COMMENT '药品名称',
    `spec`           VARCHAR(50)    DEFAULT NULL COMMENT '规格',
    `unit`           VARCHAR(20)    DEFAULT NULL COMMENT '单位',
    `quantity`       INT            DEFAULT 1 COMMENT '数量',
    `price`          DECIMAL(10, 2) DEFAULT 0.00 COMMENT '单价',
    `amount`         DECIMAL(10, 2) DEFAULT 0.00 COMMENT '小计',
    `usage`          VARCHAR(100)   DEFAULT NULL COMMENT '用法',
    `frequency`      VARCHAR(20)    DEFAULT NULL COMMENT '频次',
    `create_time`    DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`      BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`        TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_prescription_item_presc` (`prescription_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '处方明细表';

DROP TABLE IF EXISTS `biz_fee_item`;
CREATE TABLE `biz_fee_item` (
    `id`               BIGINT         NOT NULL COMMENT '主键',
    `item_code`        VARCHAR(30)   DEFAULT NULL COMMENT '项目编码',
    `item_name`        VARCHAR(100)  NOT NULL COMMENT '项目名称',
    `item_type`        TINYINT        DEFAULT 3 COMMENT '类型 1药品2诊疗3检查4床位5护理',
    `unit`             VARCHAR(20)   DEFAULT NULL COMMENT '单位',
    `price`            DECIMAL(10, 2) DEFAULT 0.00 COMMENT '价格',
    `medical_insurance` TINYINT       DEFAULT 1 COMMENT '是否医保 0否1是',
    `status`           TINYINT        DEFAULT 1 COMMENT '状态 0停用1启用',
    `create_time`      DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`        BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`          TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_fee_item_name` (`item_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '诊疗项目价格表';

-- ---------------------------------------------------------------------
-- 五、住院管理
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `biz_bed`;
CREATE TABLE `biz_bed` (
    `id`          BIGINT         NOT NULL COMMENT '主键',
    `ward_id`     BIGINT         DEFAULT NULL COMMENT '病区ID',
    `ward_name`   VARCHAR(50)   DEFAULT NULL COMMENT '病区名称',
    `dept_id`     BIGINT         DEFAULT NULL COMMENT '所属科室',
    `bed_no`      VARCHAR(20)   NOT NULL COMMENT '床位号',
    `bed_type`    TINYINT        DEFAULT 1 COMMENT '类型 1普通2重症3VIP',
    `price`       DECIMAL(10, 2) DEFAULT 0.00 COMMENT '床位费/天',
    `status`      TINYINT        DEFAULT 0 COMMENT '状态 0空闲1占用2维修',
    `patient_id`  BIGINT         DEFAULT NULL COMMENT '占用患者ID',
    `create_time` DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`   BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`     TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_bed_dept` (`dept_id`),
    KEY `idx_biz_bed_no` (`bed_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '床位表';

DROP TABLE IF EXISTS `biz_admission`;
CREATE TABLE `biz_admission` (
    `id`            BIGINT         NOT NULL COMMENT '主键',
    `admission_no`  VARCHAR(30)   NOT NULL COMMENT '住院号',
    `patient_id`    BIGINT         NOT NULL COMMENT '患者ID',
    `dept_id`       BIGINT         DEFAULT NULL COMMENT '科室ID',
    `bed_id`        BIGINT         DEFAULT NULL COMMENT '床位ID',
    `doctor_id`     BIGINT         DEFAULT NULL COMMENT '主治医生',
    `admission_date` DATE          DEFAULT NULL COMMENT '入院日期',
    `discharge_date` DATE          DEFAULT NULL COMMENT '出院日期',
    `deposit_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '押金',
    `insurance_type` TINYINT       DEFAULT 1 COMMENT '医保类型 1自费2职工3居民',
    `insurance_no`  VARCHAR(50)    DEFAULT NULL COMMENT '医保卡号',
    `diagnosis`     VARCHAR(500)   DEFAULT NULL COMMENT '入院诊断',
    `status`        TINYINT        DEFAULT 0 COMMENT '状态 0在院1出院2转科',
    `create_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`     BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`       TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_admission_no` (`admission_no`),
    KEY `idx_biz_admission_patient` (`patient_id`),
    KEY `idx_biz_admission_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '住院登记表';

DROP TABLE IF EXISTS `biz_medical_order`;
CREATE TABLE `biz_medical_order` (
    `id`           BIGINT      NOT NULL COMMENT '主键',
    `order_no`     VARCHAR(30) DEFAULT NULL COMMENT '医嘱号',
    `patient_id`   BIGINT      NOT NULL COMMENT '患者ID',
    `admission_id` BIGINT      DEFAULT NULL COMMENT '住院ID',
    `dept_id`      BIGINT      DEFAULT NULL COMMENT '科室ID',
    `doctor_id`    BIGINT      DEFAULT NULL COMMENT '开嘱医生',
    `order_type`   TINYINT     DEFAULT 1 COMMENT '类型 1长期2临时',
    `content`      VARCHAR(500) DEFAULT NULL COMMENT '医嘱内容',
    `dosage`       VARCHAR(50) DEFAULT NULL COMMENT '用量',
    `frequency`    VARCHAR(20) DEFAULT NULL COMMENT '频次',
    `start_time`   DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    `end_time`     DATETIME    DEFAULT NULL COMMENT '停止时间',
    `status`       TINYINT     DEFAULT 0 COMMENT '状态 0未执行1执行中2已停止3已完成',
    `create_time`  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    BIGINT      DEFAULT 0 COMMENT '创建人',
    `update_by`    BIGINT      DEFAULT 0 COMMENT '更新人',
    `deleted`      TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_order_admission` (`admission_id`),
    KEY `idx_biz_order_patient` (`patient_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '医嘱表';

DROP TABLE IF EXISTS `biz_order_execution`;
CREATE TABLE `biz_order_execution` (
    `id`           BIGINT   NOT NULL COMMENT '主键',
    `order_id`     BIGINT   NOT NULL COMMENT '医嘱ID',
    `patient_id`   BIGINT   DEFAULT NULL COMMENT '患者ID',
    `execute_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    `executor_id`  BIGINT   DEFAULT NULL COMMENT '执行人',
    `result`       VARCHAR(500) DEFAULT NULL COMMENT '执行结果',
    `status`       TINYINT  DEFAULT 1 COMMENT '状态 0已停止1已执行',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    BIGINT   DEFAULT 0 COMMENT '创建人',
    `update_by`    BIGINT   DEFAULT 0 COMMENT '更新人',
    `deleted`      TINYINT  DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_order_execution_order` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '医嘱执行记录表';

DROP TABLE IF EXISTS `biz_nursing_record`;
CREATE TABLE `biz_nursing_record` (
    `id`                BIGINT   NOT NULL COMMENT '主键',
    `patient_id`        BIGINT   NOT NULL COMMENT '患者ID',
    `admission_id`      BIGINT   DEFAULT NULL COMMENT '住院ID',
    `dept_id`           BIGINT   DEFAULT NULL COMMENT '科室ID',
    `record_type`       TINYINT  DEFAULT 1 COMMENT '类型 1体温单2生命体征3护理记录4出入量',
    `temperature`       DECIMAL(4, 1) DEFAULT NULL COMMENT '体温（℃）',
    `systolic_pressure` INT      DEFAULT NULL COMMENT '收缩压（mmHg）',
    `diastolic_pressure` INT     DEFAULT NULL COMMENT '舒张压（mmHg）',
    `heart_rate`        INT      DEFAULT NULL COMMENT '心率（次/分）',
    `respiration`       INT      DEFAULT NULL COMMENT '呼吸（次/分）',
    `blood_oxygen`      DECIMAL(4, 1) DEFAULT NULL COMMENT '血氧饱和度（%）',
    `content`           TEXT     COMMENT '记录内容',
    `record_time`       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    `nurse_id`          BIGINT   DEFAULT NULL COMMENT '护士ID',
    `create_time`       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         BIGINT   DEFAULT 0 COMMENT '创建人',
    `update_by`         BIGINT   DEFAULT 0 COMMENT '更新人',
    `deleted`           TINYINT  DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_nursing_patient` (`patient_id`),
    KEY `idx_biz_nursing_time` (`record_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '护理记录表';

DROP TABLE IF EXISTS `biz_course_record`;
CREATE TABLE `biz_course_record` (
    `id`           BIGINT   NOT NULL COMMENT '主键',
    `patient_id`   BIGINT   NOT NULL COMMENT '患者ID',
    `admission_id` BIGINT   DEFAULT NULL COMMENT '住院ID',
    `dept_id`      BIGINT   DEFAULT NULL COMMENT '科室ID',
    `record_type`  TINYINT  DEFAULT 1 COMMENT '类型 1首次病程2日常病程3阶段小结4出院小结',
    `title`        VARCHAR(100) DEFAULT NULL COMMENT '标题',
    `content`      TEXT     COMMENT '内容',
    `record_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    `doctor_id`    BIGINT   DEFAULT NULL COMMENT '记录医生',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    BIGINT   DEFAULT 0 COMMENT '创建人',
    `update_by`    BIGINT   DEFAULT 0 COMMENT '更新人',
    `deleted`      TINYINT  DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_course_patient` (`patient_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '住院病程记录表';

DROP TABLE IF EXISTS `biz_settlement`;
CREATE TABLE `biz_settlement` (
    `id`               BIGINT         NOT NULL COMMENT '主键',
    `settlement_no`    VARCHAR(30)   NOT NULL COMMENT '结算单号',
    `admission_id`     BIGINT         DEFAULT NULL COMMENT '住院ID',
    `patient_id`       BIGINT         NOT NULL COMMENT '患者ID',
    `dept_id`          BIGINT         DEFAULT NULL COMMENT '科室ID',
    `total_amount`     DECIMAL(10, 2) DEFAULT 0.00 COMMENT '总费用',
    `insurance_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '医保支付',
    `self_amount`      DECIMAL(10, 2) DEFAULT 0.00 COMMENT '自费金额',
    `deposit_amount`   DECIMAL(10, 2) DEFAULT 0.00 COMMENT '押金',
    `refund_amount`    DECIMAL(10, 2) DEFAULT 0.00 COMMENT '退还金额',
    `status`           TINYINT        DEFAULT 0 COMMENT '状态 0待结算1已结算2已退费',
    `settle_time`      DATETIME       DEFAULT NULL COMMENT '结算时间',
    `refund_time`      DATETIME       DEFAULT NULL COMMENT '退费时间',
    `refund_reason`    VARCHAR(255)   DEFAULT NULL COMMENT '退费原因',
    `insurance_trade_no` VARCHAR(64)  DEFAULT NULL COMMENT '医保交易流水号',
    `create_time`      DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`        BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`          TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_settlement_no` (`settlement_no`),
    KEY `idx_biz_settlement_admission` (`admission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '住院结算表';

-- ---------------------------------------------------------------------
-- 六、收费与财务
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `biz_charge`;
CREATE TABLE `biz_charge` (
    `id`               BIGINT         NOT NULL COMMENT '主键',
    `charge_no`        VARCHAR(30)   NOT NULL COMMENT '收费单号',
    `patient_id`       BIGINT         NOT NULL COMMENT '患者ID',
    `dept_id`          BIGINT         DEFAULT NULL COMMENT '科室ID',
    `visit_id`         BIGINT         DEFAULT NULL COMMENT '就诊ID(挂号记录ID)',
    `prescription_id`  BIGINT         DEFAULT NULL COMMENT '处方ID',
    `visit_type`       TINYINT        DEFAULT 1 COMMENT '类型 1挂号2门诊3住院',
    `total_amount`     DECIMAL(10, 2) DEFAULT 0.00 COMMENT '总金额',
    `discount_amount`  DECIMAL(10, 2) DEFAULT 0.00 COMMENT '优惠金额',
    `actual_amount`    DECIMAL(10, 2) DEFAULT 0.00 COMMENT '实收金额',
    `pay_type`         TINYINT        DEFAULT 1 COMMENT '支付方式 1现金2微信3支付宝4医保5银行卡',
    `pay_status`       TINYINT        DEFAULT 0 COMMENT '状态 0待支付1已支付2已退费',
    `operator_id`      BIGINT         DEFAULT NULL COMMENT '收费员',
    `charge_time`      DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '收费时间',
    `create_time`      DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`        BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`          TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_charge_no` (`charge_no`),
    KEY `idx_biz_charge_patient` (`patient_id`),
    KEY `idx_biz_charge_time` (`charge_time`),
    KEY `idx_charge_visit` (`visit_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收费单表';

DROP TABLE IF EXISTS `biz_charge_item`;
CREATE TABLE `biz_charge_item` (
    `id`        BIGINT         NOT NULL COMMENT '主键',
    `charge_id` BIGINT         NOT NULL COMMENT '收费单ID',
    `item_type` TINYINT        DEFAULT 2 COMMENT '类型 1药品2诊疗3检查4床位5护理',
    `item_id`   BIGINT         DEFAULT NULL COMMENT '项目ID',
    `item_name` VARCHAR(100)   DEFAULT NULL COMMENT '项目名称',
    `quantity`  INT            DEFAULT 1 COMMENT '数量',
    `price`     DECIMAL(10, 2) DEFAULT 0.00 COMMENT '单价',
    `amount`    DECIMAL(10, 2) DEFAULT 0.00 COMMENT '金额',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by` BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`   TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_charge_item_charge` (`charge_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收费明细表';

DROP TABLE IF EXISTS `biz_refund`;
CREATE TABLE `biz_refund` (
    `id`           BIGINT         NOT NULL COMMENT '主键',
    `refund_no`    VARCHAR(30)   NOT NULL COMMENT '退费单号',
    `charge_id`    BIGINT         NOT NULL COMMENT '收费单ID',
    `patient_id`   BIGINT         DEFAULT NULL COMMENT '患者ID',
    `refund_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '退费金额',
    `refund_type`  TINYINT        DEFAULT 1 COMMENT '类型 1退挂号费2退检查费3退药费4其他',
    `status`       TINYINT        DEFAULT 0 COMMENT '状态 0待审核1已审核2已退费3已驳回',
    `auditor_id`   BIGINT         DEFAULT NULL COMMENT '审核人',
    `audit_time`   DATETIME       DEFAULT NULL COMMENT '审核时间',
    `refund_time`  DATETIME       DEFAULT NULL COMMENT '退费时间',
    `reason`       VARCHAR(255)   DEFAULT NULL COMMENT '退费原因',
    `create_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`    BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`      TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_refund_no` (`refund_no`),
    KEY `idx_biz_refund_charge` (`charge_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '退费表';

DROP TABLE IF EXISTS `biz_prescription_return`;

CREATE TABLE `biz_prescription_return` (
    `id`              BIGINT         NOT NULL COMMENT '主键',
    `prescription_id` BIGINT         NOT NULL COMMENT '处方ID',
    `item_id`         BIGINT         NOT NULL COMMENT '处方明细ID',
    `drug_id`         BIGINT         DEFAULT NULL COMMENT '药品ID',
    `quantity`        INT            DEFAULT 1 COMMENT '退回数量',
    `amount`          DECIMAL(10, 2) DEFAULT 0.00 COMMENT '退回金额',
    `refund_id`       BIGINT         DEFAULT NULL COMMENT '关联退费单ID',
    `warehouse_id`    BIGINT         DEFAULT NULL COMMENT '退回库房ID',
    `reason`          VARCHAR(255)   DEFAULT NULL COMMENT '退药原因',
    `create_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`       BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`         TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_presc_return_presc` (`prescription_id`),
    KEY `idx_biz_presc_return_item` (`item_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '处方退药明细表';

DROP TABLE IF EXISTS `biz_invoice`;
CREATE TABLE `biz_invoice` (
    `id`          BIGINT         NOT NULL COMMENT '主键',
    `invoice_no`  VARCHAR(50)   NOT NULL COMMENT '发票号',
    `invoice_code` VARCHAR(50)   DEFAULT NULL COMMENT '发票代码',
    `charge_id`   BIGINT         DEFAULT NULL COMMENT '收费单ID',
    `patient_id`  BIGINT         DEFAULT NULL COMMENT '患者ID',
    `title`       VARCHAR(100)   DEFAULT NULL COMMENT '抬头',
    `tax_no`      VARCHAR(50)    DEFAULT NULL COMMENT '税号',
    `amount`      DECIMAL(10, 2) DEFAULT 0.00 COMMENT '金额',
    `invoice_type` TINYINT       DEFAULT 1 COMMENT '类型 1电子2纸质',
    `status`      TINYINT        DEFAULT 1 COMMENT '状态 0作废1正常',
    `invoice_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '开票时间',
    `void_reason` VARCHAR(255)   DEFAULT NULL COMMENT '作废原因',
    `create_time` DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`   BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`     TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_invoice_no` (`invoice_no`),
    KEY `idx_biz_invoice_charge` (`charge_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '发票表';

-- ---------------------------------------------------------------------
-- 七、药品与药房
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `biz_drug`;
CREATE TABLE `biz_drug` (
    `id`             BIGINT         NOT NULL COMMENT '主键',
    `drug_code`      VARCHAR(30)   NOT NULL COMMENT '药品编码',
    `drug_name`      VARCHAR(100)  NOT NULL COMMENT '药品名称',
    `generic_name`   VARCHAR(100)  DEFAULT NULL COMMENT '通用名',
    `spec`           VARCHAR(50)   DEFAULT NULL COMMENT '规格',
    `dosage_form`    VARCHAR(30)   DEFAULT NULL COMMENT '剂型',
    `unit`           VARCHAR(20)   DEFAULT NULL COMMENT '单位',
    `manufacturer`   VARCHAR(100)  DEFAULT NULL COMMENT '生产厂家',
    `approval_no`    VARCHAR(50)   DEFAULT NULL COMMENT '批准文号',
    `purchase_price` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '进价',
    `sale_price`     DECIMAL(10, 2) DEFAULT 0.00 COMMENT '售价',
    `category`       VARCHAR(50)   DEFAULT NULL COMMENT '分类',
    `is_special`     TINYINT        DEFAULT 0 COMMENT '是否特殊药品 0否1是',
    `status`         TINYINT        DEFAULT 1 COMMENT '状态 0停用1启用',
    `create_time`    DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      BIGINT         DEFAULT 0 COMMENT '创建人',
    `update_by`      BIGINT         DEFAULT 0 COMMENT '更新人',
    `deleted`        TINYINT        DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_drug_code` (`drug_code`),
    KEY `idx_biz_drug_name` (`drug_name`),
    KEY `idx_biz_drug_category` (`category`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '药品目录表';

DROP TABLE IF EXISTS `biz_warehouse`;
CREATE TABLE `biz_warehouse` (
    `id`             BIGINT       NOT NULL COMMENT '主键',
    `warehouse_code` VARCHAR(30) NOT NULL COMMENT '库房编码',
    `warehouse_name` VARCHAR(50) NOT NULL COMMENT '库房名称',
    `dept_id`        BIGINT       DEFAULT NULL COMMENT '所属科室',
    `warehouse_type` TINYINT      DEFAULT 1 COMMENT '类型 1门诊药房2住院药房3库房',
    `manager`        VARCHAR(50) DEFAULT NULL COMMENT '负责人',
    `phone`          VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `address`        VARCHAR(255) DEFAULT NULL COMMENT '地址',
    `status`         TINYINT      DEFAULT 1 COMMENT '状态 0停用1启用',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      BIGINT       DEFAULT 0 COMMENT '创建人',
    `update_by`      BIGINT       DEFAULT 0 COMMENT '更新人',
    `deleted`        TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_warehouse_code` (`warehouse_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库房表';

DROP TABLE IF EXISTS `biz_stock`;
CREATE TABLE `biz_stock` (
    `id`            BIGINT   NOT NULL COMMENT '主键',
    `drug_id`       BIGINT   NOT NULL COMMENT '药品ID',
    `batch_no`      VARCHAR(50) DEFAULT NULL COMMENT '批号',
    `quantity`      INT      DEFAULT 0 COMMENT '库存数量',
    `lock_quantity` INT      DEFAULT 0 COMMENT '锁定数量',
    `warn_num`      INT      DEFAULT 0 COMMENT '预警数量',
    `produce_date`  DATE     DEFAULT NULL COMMENT '生产日期',
    `expire_date`   DATE     DEFAULT NULL COMMENT '有效期',
    `warehouse_id`  BIGINT   DEFAULT NULL COMMENT '库房ID',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT   DEFAULT 0 COMMENT '创建人',
    `update_by`     BIGINT   DEFAULT 0 COMMENT '更新人',
    `deleted`       TINYINT  DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_stock_drug` (`drug_id`),
    KEY `idx_biz_stock_expire` (`expire_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '药品库存表';

DROP TABLE IF EXISTS `biz_stock_record`;
CREATE TABLE `biz_stock_record` (
    `id`           BIGINT   NOT NULL COMMENT '主键',
    `drug_id`      BIGINT   NOT NULL COMMENT '药品ID',
    `record_type`  TINYINT  DEFAULT 1 COMMENT '类型 1入库2出库3盘点4退药',
    `quantity`     INT      DEFAULT 0 COMMENT '数量',
    `batch_no`     VARCHAR(50) DEFAULT NULL COMMENT '批号',
    `warehouse_id` BIGINT   DEFAULT NULL COMMENT '库房ID',
    `operator_id`  BIGINT   DEFAULT NULL COMMENT '操作人',
    `biz_no`       VARCHAR(50) DEFAULT NULL COMMENT '关联单据号',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `update_time`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    BIGINT   DEFAULT 0 COMMENT '创建人',
    `update_by`    BIGINT   DEFAULT 0 COMMENT '更新人',
    `deleted`      TINYINT  DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_stock_record_drug` (`drug_id`),
    KEY `idx_biz_stock_record_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '药品出入库流水表';

-- ---------------------------------------------------------------------
-- 八、检查检验（LIS / PACS）
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `biz_exam_apply`;
CREATE TABLE `biz_exam_apply` (
    `id`                 BIGINT      NOT NULL COMMENT '主键',
    `apply_no`           VARCHAR(30) NOT NULL COMMENT '申请单号',
    `patient_id`         BIGINT      NOT NULL COMMENT '患者ID',
    `doctor_id`          BIGINT      DEFAULT NULL COMMENT '申请医生',
    `dept_id`            BIGINT      DEFAULT NULL COMMENT '申请科室',
    `visit_id`           BIGINT      DEFAULT NULL COMMENT '就诊ID(挂号记录ID)',
    `exam_type`          TINYINT     DEFAULT 1 COMMENT '类型 1检验2检查（影像）',
    `item_name`          VARCHAR(100) DEFAULT NULL COMMENT '项目名称',
    `clinical_diagnosis` VARCHAR(500) DEFAULT NULL COMMENT '临床诊断',
    `amount`            DECIMAL(10, 2) DEFAULT 0.00 COMMENT '检查费金额',
    `apply_time`         DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `status`             TINYINT     DEFAULT 0 COMMENT '状态 0待缴费1待采样2检验中3已出报告',
    `auditor_id`         BIGINT      DEFAULT NULL COMMENT '审核人',
    `audit_time`         DATETIME    DEFAULT NULL COMMENT '审核时间',
    `create_time`        DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`          BIGINT      DEFAULT 0 COMMENT '创建人',
    `update_by`          BIGINT      DEFAULT 0 COMMENT '更新人',
    `deleted`            TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_exam_apply_no` (`apply_no`),
    KEY `idx_biz_exam_apply_patient` (`patient_id`),
    KEY `idx_biz_exam_apply_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '检查申请表';

DROP TABLE IF EXISTS `biz_exam_report`;
CREATE TABLE `biz_exam_report` (
    `id`             BIGINT      NOT NULL COMMENT '主键',
    `apply_id`       BIGINT      DEFAULT NULL COMMENT '申请ID',
    `patient_id`     BIGINT      DEFAULT NULL COMMENT '患者ID',
    `report_title`   VARCHAR(100) DEFAULT NULL COMMENT '报告标题',
    `report_content` TEXT        COMMENT '检验/检查结果',
    `conclusion`     VARCHAR(500) DEFAULT NULL COMMENT '结论',
    `critical_flag`  TINYINT     DEFAULT 0 COMMENT '是否危急值 0否1是',
    `attachment`     VARCHAR(255) DEFAULT NULL COMMENT '附件地址',
    `auditor_id`     BIGINT      DEFAULT NULL COMMENT '审核人',
    `audit_time`     DATETIME    DEFAULT NULL COMMENT '审核时间',
    `report_time`    DATETIME    DEFAULT NULL COMMENT '报告时间',
    `status`         TINYINT     DEFAULT 0 COMMENT '状态 0草稿1已审核2已发布',
    `create_time`    DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      BIGINT      DEFAULT 0 COMMENT '创建人',
    `update_by`      BIGINT      DEFAULT 0 COMMENT '更新人',
    `deleted`        TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_exam_report_apply` (`apply_id`),
    KEY `idx_biz_exam_report_patient` (`patient_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '检查报告表';

DROP TABLE IF EXISTS `biz_critical_value`;
CREATE TABLE `biz_critical_value` (
    `id`              BIGINT   NOT NULL COMMENT '主键',
    `report_id`       BIGINT   DEFAULT NULL COMMENT '报告ID',
    `patient_id`      BIGINT   DEFAULT NULL COMMENT '患者ID',
    `dept_id`         BIGINT   DEFAULT NULL COMMENT '科室ID',
    `item_name`       VARCHAR(100) DEFAULT NULL COMMENT '项目名称',
    `value`           VARCHAR(100) DEFAULT NULL COMMENT '检测值',
    `reference_range` VARCHAR(100) DEFAULT NULL COMMENT '参考范围',
    `status`          TINYINT  DEFAULT 0 COMMENT '状态 0待处理1已通知2已处理',
    `notify_time`     DATETIME DEFAULT NULL COMMENT '通知时间',
    `handler_id`      BIGINT   DEFAULT NULL COMMENT '处理人',
    `handle_time`     DATETIME DEFAULT NULL COMMENT '处理时间',
    `handle_remark`   VARCHAR(500) DEFAULT NULL COMMENT '处理说明',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       BIGINT   DEFAULT 0 COMMENT '创建人',
    `update_by`       BIGINT   DEFAULT 0 COMMENT '更新人',
    `deleted`         TINYINT  DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_critical_status` (`status`),
    KEY `idx_biz_critical_patient` (`patient_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '危急值表';

-- ---------------------------------------------------------------------
-- 九、公共卫生与健康管理
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `biz_health_record`;
CREATE TABLE `biz_health_record` (
    `id`              BIGINT       NOT NULL COMMENT '主键',
    `record_no`       VARCHAR(30)  NOT NULL COMMENT '档案号',
    `name`            VARCHAR(50)  NOT NULL COMMENT '姓名',
    `gender`          TINYINT      DEFAULT 0 COMMENT '性别 0女1男',
    `birth_date`      DATE         DEFAULT NULL COMMENT '出生日期',
    `id_card`         VARCHAR(200) DEFAULT NULL COMMENT '身份证号（AES 密文）',
    `phone`           VARCHAR(200) DEFAULT NULL COMMENT '手机号（AES 密文）',
    `community`       VARCHAR(100) DEFAULT NULL COMMENT '所属社区',
    `record_type`     TINYINT      DEFAULT 1 COMMENT '档案类型 1居民健康档案2慢病档案3孕产妇4儿童',
    `chronic_type`    VARCHAR(255) DEFAULT NULL COMMENT '慢病类型（逗号分隔）',
    `medical_history` TEXT         COMMENT '既往病史',
    `family_history`  TEXT         COMMENT '家族病史',
    `create_date`     DATE         DEFAULT NULL COMMENT '建档日期',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       BIGINT       DEFAULT 0 COMMENT '创建人',
    `update_by`       BIGINT       DEFAULT 0 COMMENT '更新人',
    `deleted`         TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_health_record_no` (`record_no`),
    KEY `idx_biz_health_record_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '居民健康档案表';

DROP TABLE IF EXISTS `biz_vaccination`;
CREATE TABLE `biz_vaccination` (
    `id`               BIGINT      NOT NULL COMMENT '主键',
    `vaccination_no`   VARCHAR(30) DEFAULT NULL COMMENT '接种单号',
    `patient_id`       BIGINT      DEFAULT NULL COMMENT '患者ID',
    `name`             VARCHAR(50) DEFAULT NULL COMMENT '姓名',
    `vaccine_name`     VARCHAR(100) DEFAULT NULL COMMENT '疫苗名称',
    `batch_no`         VARCHAR(50) DEFAULT NULL COMMENT '批号',
    `dose_no`          INT         DEFAULT 1 COMMENT '剂次',
    `vaccination_date` DATE        DEFAULT NULL COMMENT '接种日期',
    `injection_site`   VARCHAR(50) DEFAULT NULL COMMENT '接种部位',
    `doctor_id`        BIGINT      DEFAULT NULL COMMENT '接种人',
    `next_date`        DATE        DEFAULT NULL COMMENT '下次接种日期',
    `status`           TINYINT     DEFAULT 1 COMMENT '状态 0取消1正常',
    `create_time`      DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        BIGINT      DEFAULT 0 COMMENT '创建人',
    `update_by`        BIGINT      DEFAULT 0 COMMENT '更新人',
    `deleted`          TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_vaccination_patient` (`patient_id`),
    KEY `idx_biz_vaccination_next` (`next_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '疫苗接种表';

DROP TABLE IF EXISTS `biz_infectious_report`;
CREATE TABLE `biz_infectious_report` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `report_no`     VARCHAR(30)  NOT NULL COMMENT '上报单号',
    `name`          VARCHAR(50)  NOT NULL COMMENT '姓名',
    `gender`        TINYINT      DEFAULT 0 COMMENT '性别 0女1男',
    `birth_date`    DATE         DEFAULT NULL COMMENT '出生日期',
    `id_card`       VARCHAR(200) DEFAULT NULL COMMENT '身份证号（AES 密文）',
    `phone`         VARCHAR(200) DEFAULT NULL COMMENT '手机号（AES 密文）',
    `address`       VARCHAR(255) DEFAULT NULL COMMENT '现住址',
    `disease_name`  VARCHAR(100) DEFAULT NULL COMMENT '传染病名称',
    `icd_code`      VARCHAR(20)  DEFAULT NULL COMMENT 'ICD编码',
    `onset_date`    DATE         DEFAULT NULL COMMENT '发病日期',
    `report_date`   DATE         DEFAULT NULL COMMENT '报告日期',
    `status`        TINYINT      DEFAULT 0 COMMENT '状态 0待上报1已上报2已审核',
    `audit_time`    DATETIME     DEFAULT NULL COMMENT '审核时间',
    `audit_remark`  VARCHAR(255) DEFAULT NULL COMMENT '审核意见',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT       DEFAULT 0 COMMENT '创建人',
    `update_by`     BIGINT       DEFAULT 0 COMMENT '更新人',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_infectious_report_no` (`report_no`),
    KEY `idx_biz_infectious_disease` (`disease_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '传染病上报表';

DROP TABLE IF EXISTS `biz_physical_exam`;
CREATE TABLE `biz_physical_exam` (
    `id`            BIGINT      NOT NULL COMMENT '主键',
    `exam_no`       VARCHAR(30) DEFAULT NULL COMMENT '体检号',
    `patient_id`    BIGINT      DEFAULT NULL COMMENT '患者ID',
    `name`          VARCHAR(50) DEFAULT NULL COMMENT '姓名',
    `package_name`  VARCHAR(100) DEFAULT NULL COMMENT '体检套餐',
    `exam_date`     DATE        DEFAULT NULL COMMENT '体检日期',
    `result`        TEXT        COMMENT '体检结果',
    `assessment`    TEXT        COMMENT '健康评估',
    `score`         INT         DEFAULT 0 COMMENT '健康评分',
    `status`        TINYINT     DEFAULT 1 COMMENT '状态 0待出报告1已出报告',
    `create_time`   DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT      DEFAULT 0 COMMENT '创建人',
    `update_by`     BIGINT      DEFAULT 0 COMMENT '更新人',
    `deleted`       TINYINT     DEFAULT 0 COMMENT '逻辑删除 0否1是',
    PRIMARY KEY (`id`),
    KEY `idx_biz_physical_exam_patient` (`patient_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '体检记录表';

SET FOREIGN_KEY_CHECKS = 1;
