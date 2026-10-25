-- =====================================================================
--  增量迁移脚本：医生补「检查检验 / 住院登记」菜单与按钮权限
--  适用：已按 healthcare.sql 初始化过的库，直接执行本脚本即可
--  执行：mysql -uroot -p healthcare < migration-20261025-doctor-inpatient-exam.sql
--  幂等：每条 INSERT 都带 NOT EXISTS 判断，重复执行不会报错
-- =====================================================================

USE `healthcare`;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 背景
--   医生(role_id=3) 原本只有门诊权限（写病历 / 开处方）。
--   实际诊疗中，医生接诊后常需要：
--     · 开检查单（检查检验 → 检查申请，按钮 exam:apply:add=70101）
--     · 判断患者需住院并登记入院（住院管理 → 住院登记，按钮 inpatient:admission:add=50201）
--   原授权里 701/70101 已存在，却缺父级容器 70，导致侧边栏无法正确归组；
--   住院相关 50/502/50201 则整条缺失，医生根本看不到入口。
--   这里补齐这 4 个菜单节点（M/C/F 各司其职，F 行只作为权限串，不进侧边栏）。
-- ---------------------------------------------------------------------

SET @row := (SELECT IFNULL(MAX(`id`), 0) FROM `sys_role_menu`);

-- 70 检查检验（M，父级容器，归组 701 等已有菜单）
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 3, 70 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 3 AND `menu_id` = 70);

-- 50 住院管理（M，父级容器）
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 3, 50 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 3 AND `menu_id` = 50);

-- 502 住院登记（C，页面入口 /inpatient/admission，perms=inpatient:admission:list）
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 3, 502 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 3 AND `menu_id` = 502);

-- 50201 入院登记（F，按钮权限 inpatient:admission:add）
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 3, 50201 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 3 AND `menu_id` = 50201);

-- ---------------------------------------------------------------------
-- 补 patient:list（患者查询）给医生与护士
--   住院登记页(inpatient/admission) 一打开就要调 /patient/page 拉患者下拉，
--   该接口 @PreAuthorize 要求 patient:list。迁移 20261025 只给收费员(cashier) 补了这条，
--   导致医生(乃至原本就能进住院登记的护士)打开登记页时患者下拉是空的、无法登记。
--   这里把 30204(F, patient:list) 也授予医生(3)与护士(4)，与收费员保持一致。
-- ---------------------------------------------------------------------

-- 确保菜单行 30204 存在（若此前迁移未执行过则补建，F 行不进侧边栏）
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
SELECT 30204, '患者查询', 302, 'F', NULL, NULL, 'patient:list', NULL, 4, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 30204);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 3, 30204 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 3 AND `menu_id` = 30204);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 4, 30204 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 4 AND `menu_id` = 30204);

-- ---------------------------------------------------------------------
-- 补 inpatient:bed:list（床位查询）给医生
--   住院登记页(inpatient/admission) 打开时还要调 listBeds() 拉床位下拉，
--   对应接口 @PreAuthorize 要求 inpatient:bed:list(50204)。
--   医生此前只有 patient:list，没有这条，导致点"建议住院"时既弹"无权限"、
--   床位下拉又全空、报"没有床位可以选择"。这里补上 50204。
-- ---------------------------------------------------------------------
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 3, 50204 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 3 AND `menu_id` = 50204);
