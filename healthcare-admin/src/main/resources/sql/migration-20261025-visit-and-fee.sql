-- =====================================================================
--  增量迁移脚本：门诊全流程贯通（就诊ID / 挂号费入账 / 检查费联动）
--  适用：已按 healthcare.sql 初始化过的库，直接执行本脚本即可
--  执行：mysql -uroot -p healthcare < migration-20261025-visit-and-fee.sql
--  幂等：每条 ALTER 前先查 information_schema，重复执行不会报错
-- =====================================================================

USE `healthcare`;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 就诊ID 贯通
--    biz_registration.id 是「一次就诊」的唯一标识，处方/病历/收费单都必须挂上它，
--    否则同一患者一天挂两次号，三条线的数据就串不到一起。
-- ---------------------------------------------------------------------
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_medical_record' AND COLUMN_NAME = 'visit_id') = 0,
    'ALTER TABLE `biz_medical_record` ADD COLUMN `visit_id` BIGINT DEFAULT NULL COMMENT ''就诊ID(挂号记录ID)'' AFTER `dept_id`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_prescription' AND COLUMN_NAME = 'visit_id') = 0,
    'ALTER TABLE `biz_prescription` ADD COLUMN `visit_id` BIGINT DEFAULT NULL COMMENT ''就诊ID(挂号记录ID)'' AFTER `dept_id`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_charge' AND COLUMN_NAME = 'visit_id') = 0,
    'ALTER TABLE `biz_charge` ADD COLUMN `visit_id` BIGINT DEFAULT NULL COMMENT ''就诊ID(挂号记录ID)'' AFTER `dept_id`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 就诊ID 查询索引（按就诊查处方 / 查收费单是高频操作）
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_prescription' AND INDEX_NAME = 'idx_presc_visit') = 0,
    'ALTER TABLE `biz_prescription` ADD INDEX `idx_presc_visit` (`visit_id`)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_charge' AND INDEX_NAME = 'idx_charge_visit') = 0,
    'ALTER TABLE `biz_charge` ADD INDEX `idx_charge_visit` (`visit_id`)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_medical_record' AND INDEX_NAME = 'idx_record_visit') = 0,
    'ALTER TABLE `biz_medical_record` ADD INDEX `idx_record_visit` (`visit_id`)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 检查申请也必须知道属于哪次就诊，否则收费台无法判断该不该收这张单
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_exam_apply' AND COLUMN_NAME = 'visit_id') = 0,
    'ALTER TABLE `biz_exam_apply` ADD COLUMN `visit_id` BIGINT DEFAULT NULL COMMENT ''就诊ID(挂号记录ID)'' AFTER `dept_id`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. 检查费金额
--    原来申请单上没有钱，收费台只能靠"手工确认缴费"糊过去，钱根本不入账。
-- ---------------------------------------------------------------------
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_exam_apply' AND COLUMN_NAME = 'amount') = 0,
    'ALTER TABLE `biz_exam_apply` ADD COLUMN `amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT ''检查费金额'' AFTER `clinical_diagnosis`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 3. 历史数据回填
--    老数据没有 visit_id，按「患者 + 科室 + 医生 + 当天」尽量补，
--    补不上的留空（查询时按 visit_id 过滤会漏掉这部分，属已知限制）。
-- ---------------------------------------------------------------------
UPDATE `biz_prescription` p
    JOIN `biz_registration` r
      ON r.`patient_id` = p.`patient_id`
     AND r.`doctor_id` = p.`doctor_id`
     AND DATE(r.`reg_time`) = DATE(p.`create_time`)
     AND r.`deleted` = 0
SET p.`visit_id` = r.`id`
WHERE p.`visit_id` IS NULL AND p.`deleted` = 0;

UPDATE `biz_medical_record` m
    JOIN `biz_registration` r
      ON r.`patient_id` = m.`patient_id`
     AND r.`doctor_id` = m.`doctor_id`
     AND DATE(r.`reg_time`) = DATE(m.`visit_time`)
     AND r.`deleted` = 0
SET m.`visit_id` = r.`id`
WHERE m.`visit_id` IS NULL AND m.`deleted` = 0;

UPDATE `biz_charge` c
    JOIN `biz_prescription` p ON p.`id` = c.`prescription_id`
SET c.`visit_id` = p.`visit_id`
WHERE c.`visit_id` IS NULL AND c.`prescription_id` IS NOT NULL AND p.`visit_id` IS NOT NULL;

-- ---------------------------------------------------------------------
-- 4. 修正历史脏数据：早期"作废处方"被写成 3（已发药），
--    药房会把它当成待退药单据。这里按「已作废」的新约定改为 5。
--    判定依据：状态 3 但没有任何已支付的收费单指向它。
-- ---------------------------------------------------------------------
UPDATE `biz_prescription` p
SET p.`status` = 5
WHERE p.`status` = 3
  AND p.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1 FROM `biz_charge` c
      WHERE c.`prescription_id` = p.`id` AND c.`pay_status` = 1 AND c.`deleted` = 0
  );

-- ---------------------------------------------------------------------
-- 5. 预约转挂号打通
--    线上预约与线下挂号共用同一个号源池。预约成功即占号（remain_num - 1），
--    到院「取号」只是把预约转成正式挂号记录，不再重复扣号。
--    registration_id 就是这条线索：取号后回写，退号时据此回退预约状态。
-- ---------------------------------------------------------------------
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_appointment'
       AND COLUMN_NAME = 'registration_id') = 0,
    'ALTER TABLE `biz_appointment` ADD COLUMN `registration_id` BIGINT DEFAULT NULL COMMENT ''转挂号后的挂号记录ID'' AFTER `schedule_id`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_appointment'
       AND INDEX_NAME = 'idx_appt_registration') = 0,
    'ALTER TABLE `biz_appointment` ADD INDEX `idx_appt_registration` (`registration_id`)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回填：历史上已经「已就诊」的预约，按患者 + 医生 + 同日尽量挂到对应挂号记录上
UPDATE `biz_appointment` a
    JOIN `biz_registration` r
      ON r.`patient_id` = a.`patient_id`
     AND r.`doctor_id` = a.`doctor_id`
     AND DATE(r.`reg_time`) = a.`appointment_date`
     AND r.`deleted` = 0
SET a.`registration_id` = r.`id`
WHERE a.`registration_id` IS NULL
  AND a.`status` = 1
  AND a.`deleted` = 0;

-- ---------------------------------------------------------------------
-- 6. 结算退费留痕
--    原来结算只有 0待结算 / 1已结算，退费状态 2 永远到不了。
--    退费必须能查明时间与原因，补两列。
-- ---------------------------------------------------------------------
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_settlement'
       AND COLUMN_NAME = 'refund_time') = 0,
    'ALTER TABLE `biz_settlement` ADD COLUMN `refund_time` DATETIME DEFAULT NULL COMMENT ''退费时间'' AFTER `settle_time`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_settlement'
       AND COLUMN_NAME = 'refund_reason') = 0,
    'ALTER TABLE `biz_settlement` ADD COLUMN `refund_reason` VARCHAR(255) DEFAULT NULL COMMENT ''退费原因'' AFTER `refund_time`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 7. 传染病审核留痕
--    原来 0待上报 / 2已审核 两个状态都不可达：保存就写 1，之后没有任何入口改状态。
--    补审核时间与审核意见，并把历史上写死的 1 分成「待上报」与「已上报」两条路走。
-- ---------------------------------------------------------------------
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_infectious_report'
       AND COLUMN_NAME = 'audit_time') = 0,
    'ALTER TABLE `biz_infectious_report` ADD COLUMN `audit_time` DATETIME DEFAULT NULL COMMENT ''审核时间'' AFTER `status`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = 'healthcare' AND TABLE_NAME = 'biz_infectious_report'
       AND COLUMN_NAME = 'audit_remark') = 0,
    'ALTER TABLE `biz_infectious_report` ADD COLUMN `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT ''审核意见'' AFTER `audit_time`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 8. 取消处方审核环节
--    处方是医生自己开的医嘱，开方即生效，不需要药师/医师再审核一遍。
--    状态 0 的语义由「待审核」改为「待收费」，历史 status=1「已审核」统一并回 0；
--    收费台从此直接检索 0 待收费的处方即可收费。
-- ---------------------------------------------------------------------
UPDATE `biz_prescription` SET `status` = 0 WHERE `status` = 1 AND `deleted` = 0;

ALTER TABLE `biz_prescription`
    MODIFY COLUMN `status` TINYINT DEFAULT 0 COMMENT '状态 0待收费1已审核(历史)2已收费3已发药4已退5已作废';

-- 审核按钮与接口已下线，对应按钮权限一并回收
DELETE FROM `sys_role_menu` WHERE `menu_id` = 40202;
DELETE FROM `sys_menu` WHERE `id` = 40202;

-- ---------------------------------------------------------------------
-- 9. 退药与退费打通
--    原来药房「退药」只把药加回库存、把处方置成 4 已退，收费单仍是已支付、
--    退费表里查不到任何记录——药退了钱没退，账对不上。
--    这里补一张退药明细表：记录每次退了哪条处方明细，用来
--    (1) 判断哪些药已经退过，防止重复退药；
--    (2) 判断是否整张处方全部退回，只有全退完处方才进入终态「已退」，
--        部分退药时处方保持「已发药」，病人剩下的药还能继续退。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `biz_prescription_return` (
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

-- ---------------------------------------------------------------------
-- 10. 补一线角色缺失的"查询类"权限
--     这几处不是页面没授权，而是角色能进页面、页面初始化却要调管理类接口，
--     结果一进去就 403「无权限」，下拉/列表全空：
--       cashier    挂号管理 → 选患者   需要 patient:list
--       cashier    收费管理 → 读处方   需要 clinic:prescription:list
--       doctor     检查申请 → 选诊疗项目 需要 charge:feeItem:list
--       nurse      住院登记 → 选床位   需要 inpatient:bed:list
--       pharmacist 发药退药 → 拉处方列表 需要 clinic:prescription:list
--     这里用 F 类型权限行（menu_type='F'）单独授予，F 行不进侧边栏，
--     只作为权限串参与 @ss.hasPermi 判断，不改变各角色可见的菜单。
-- ---------------------------------------------------------------------
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
SELECT 30204, '患者查询', 302, 'F', NULL, NULL, 'patient:list', NULL, 4, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 30204);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
SELECT 50204, '床位查询', 502, 'F', NULL, NULL, 'inpatient:bed:list', NULL, 4, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 50204);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
SELECT 60403, '处方查询', 604, 'F', NULL, NULL, 'clinic:prescription:list', NULL, 3, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 60403);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
SELECT 70103, '诊疗项目查询', 701, 'F', NULL, NULL, 'charge:feeItem:list', NULL, 3, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 70103);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
SELECT 80103, '处方查看', 801, 'F', NULL, NULL, 'clinic:prescription:list', NULL, 3, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 80103);

SET @row := (SELECT IFNULL(MAX(`id`), 0) FROM `sys_role_menu`);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 3, 70103 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 3 AND `menu_id` = 70103);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 4, 50204 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 4 AND `menu_id` = 50204);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 5, 30204 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 5 AND `menu_id` = 30204);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 5, 80103 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 5 AND `menu_id` = 80103);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 6, 60403 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 60403);

-- ---------------------------------------------------------------------
-- 11. 补"挂号页选排班"与"收费台选检查单"的查询权限
--       cashier / doctor 挂号管理：选号源要读排班 → registration:schedule:list
--       cashier 收费管理：加检查单收费要读检查申请 → exam:apply:list
--     同样是 F 类型权限行，只加权限串，不给菜单入口。
-- ---------------------------------------------------------------------
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
SELECT 30205, '排班查询', 302, 'F', NULL, NULL, 'registration:schedule:list', NULL, 5, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 30205);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
SELECT 80104, '检查单查看', 801, 'F', NULL, NULL, 'exam:apply:list', NULL, 4, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 80104);

SET @row := (SELECT IFNULL(MAX(`id`), 0) FROM `sys_role_menu`);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 3, 30205 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 3 AND `menu_id` = 30205);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 5, 30205 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 5 AND `menu_id` = 30205);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 5, 80104 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 5 AND `menu_id` = 80104);

-- 医生需要在叫号大屏叫号：补 registration:call（30203）
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 3, 30203 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 3 AND `menu_id` = 30203);

-- ---------------------------------------------------------------------
-- 12. 医生的数据范围收紧到"仅本人"
--      原来 doctor 的 data_scope 也是 2（本部门及以下），等于医生能看到整个内科
--      所有医生的患者。改成 4 之后，DataScopeHelper 才会把接诊队列、病历、处方、
--      检查单、患者下拉统统收窄到"挂在他自己名下"的那部分。
--      收费员/药师/护士保持 2 不动：他们的业务本来就是全院流水，收窄会直接查不到单。
-- ---------------------------------------------------------------------
UPDATE `sys_role` SET `data_scope` = 4 WHERE `role_code` = 'doctor';
