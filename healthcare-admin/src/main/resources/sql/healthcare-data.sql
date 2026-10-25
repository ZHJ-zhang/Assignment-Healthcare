-- =====================================================================
--  医疗后台管理系统 初始化数据
--  执行顺序：先 healthcare.sql（建表），再 healthcare-data.sql（初始化数据）
--
--  默认口令：admin / doc01 / nurse01 / cash01 / pharma01 / report01 均为 123456
--  BCrypt 哈希由 BCryptPasswordEncoder 生成，可直接用于登录验证。
-- =====================================================================

USE `healthcare`;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 一、科室
-- ---------------------------------------------------------------------
DELETE FROM `sys_dept`;
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `dept_type`, `phone`, `sort`, `leader`, `status`) VALUES
(1, 0, '医院总部', 'HQ', 4, '0755-88880000', 1, '院长', 1),
(2, 1, '内科', 'NK', 1, '0755-88880001', 1, '李主任', 1),
(3, 1, '外科', 'WK', 1, '0755-88880002', 2, '王主任', 1),
(4, 1, '儿科', 'EK', 1, '0755-88880003', 3, '赵主任', 1),
(5, 1, '妇产科', 'FK', 1, '0755-88880004', 4, '钱主任', 1),
(6, 1, '中医科', 'ZK', 1, '0755-88880005', 5, '孙主任', 1),
(7, 1, '急诊科', 'JK', 1, '0755-88880006', 6, '周主任', 1),
(8, 1, '检验科', 'JYK', 3, '0755-88880007', 7, '吴主管', 1),
(9, 1, '影像科', 'YXK', 3, '0755-88880008', 8, '郑主管', 1),
(10, 1, '门诊药房', 'MPYF', 5, '0755-88880009', 9, '何药师', 1),
(11, 1, '住院收费处', 'ZYSF', 4, '0755-88880010', 10, '收费组长', 1),
(12, 1, '公共卫生科', 'GQWSK', 4, '0755-88880011', 11, '林主任', 1);

-- ---------------------------------------------------------------------
-- 二、角色
-- ---------------------------------------------------------------------
DELETE FROM `sys_role`;
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `sort`, `data_scope`, `remark`, `status`) VALUES
(1, '超级管理员', 'admin', 1, 1, '拥有全部权限', 1),
(2, '系统管理员', 'system', 2, 1, '负责系统管理与审计', 1),
(3, '医生', 'doctor', 3, 4, '接诊、开处方、开检查单；只看自己名下的患者', 1),
(4, '护士', 'nurse', 4, 2, '护理记录、医嘱执行', 1),
(5, '收费员', 'cashier', 5, 2, '挂号收费、退费、发票', 1),
(6, '药师', 'pharmacist', 6, 2, '药品目录、库存、发药', 1),
(7, '统计员', 'reporter', 7, 1, '统计报表查看与导出', 1);

-- ---------------------------------------------------------------------
-- 三、用户
-- ---------------------------------------------------------------------
DELETE FROM `sys_user`;
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `gender`, `phone`, `phone_hash`, `email`, `dept_id`, `status`, `remark`, `deleted`) VALUES
(1, 'admin', '$2a$10$RrN0MFp8h3taRtR.EMvQhulW/3lBcbhc5xVrZEMII2SxigzstXNFe', '系统管理员', 1, '86ieu4De7h+ZC9eBZJTwlw==', '359ea74a80a57accd42a7311ed96eca04f3e631d0ab34ea76808c543240d8a68', 'admin@healthcare.local', 1, 1, '内置超级管理员', 0),
(2, 'system', '$2a$10$RrN0MFp8h3taRtR.EMvQhulW/3lBcbhc5xVrZEMII2SxigzstXNFe', '系统管理员', 1, 'uhTG+AaQ3wEVcXekTiLSXw==', '5158c094948be1fae23c5b720ba3d486572b5c3912b06bb209f31be2b7ddaf6f', 'system@healthcare.local', 1, 1, '系统管理', 0),
(3, 'doc01', '$2a$10$RrN0MFp8h3taRtR.EMvQhulW/3lBcbhc5xVrZEMII2SxigzstXNFe', '李医生', 1, 'piFOsUj5AhQ0hFhCK9QyaQ==', '17001a6a9f9ec4ba803f9495e3f4880bce58b7d330f1ccb947d546ba8437de12', 'doc01@healthcare.local', 2, 1, '内科医生', 0),
(4, 'nurse01', '$2a$10$RrN0MFp8h3taRtR.EMvQhulW/3lBcbhc5xVrZEMII2SxigzstXNFe', '陈护士', 0, 'iuEDUBF7qSJGSsAKpiiwUg==', 'fbab935ac19ac01bca0ee700e17a3c4d27b26cc81e3510b6dc834929397fe11f', 'nurse01@healthcare.local', 2, 1, '内科护士', 0),
(5, 'cash01', '$2a$10$RrN0MFp8h3taRtR.EMvQhulW/3lBcbhc5xVrZEMII2SxigzstXNFe', '收费员小周', 0, 'koPpRSGhexEGMKdN8TnqXQ==', '2b072d9822ce8be1d9b9b6039a485876b9fcf129bb0bd3786310008389e2d423', 'cash01@healthcare.local', 11, 1, '门诊收费', 0),
(6, 'pharma01', '$2a$10$RrN0MFp8h3taRtR.EMvQhulW/3lBcbhc5xVrZEMII2SxigzstXNFe', '药师小何', 0, 'GeRcB4UTnSPrryJbPDb/uQ==', '29d7c60281b7ac6722cc2c1cfc5e109f92ab7f9c2e3bf057d86f3876165884c1', 'pharma01@healthcare.local', 10, 1, '门诊药房', 0),
(7, 'report01', '$2a$10$RrN0MFp8h3taRtR.EMvQhulW/3lBcbhc5xVrZEMII2SxigzstXNFe', '统计员小孙', 0, 'BDV2H/VFwpCagcPY66A20Q==', '6c6936fd029e108eec1485a7e2f3beb955b61c7738e43309148bcf2393f34e50', 'report01@healthcare.local', 1, 1, '统计报表', 0);

DELETE FROM `sys_user_role`;
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES
(1, 1, 1),
(2, 2, 2),
(3, 3, 3),
(4, 4, 4),
(5, 5, 5),
(6, 6, 6),
(7, 7, 7);

-- ---------------------------------------------------------------------
-- 四、安全配置 / 系统参数
-- ---------------------------------------------------------------------
DELETE FROM `sys_security_config`;
INSERT INTO `sys_security_config` (`id`, `login_fail_limit`, `lock_minutes`, `session_timeout`, `ip_whitelist`, `two_factor_enabled`, `mask_enabled`, `password_min_length`, `password_expire_days`) VALUES
(1, 5, 30, 30, NULL, 0, 1, 6, 90);

DELETE FROM `sys_config`;
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `built_in`, `remark`) VALUES
(1, '患者编号前缀', 'patient.no.prefix', 'P', 1, '患者编号自动生成前缀'),
(2, '挂号单号前缀', 'registration.no.prefix', 'REG', 1, '挂号单号自动生成前缀'),
(3, '处方单号前缀', 'prescription.no.prefix', 'PR', 1, '处方单号自动生成前缀'),
(4, '收费单号前缀', 'charge.no.prefix', 'CH', 1, '收费单号自动生成前缀'),
(5, '住院号前缀', 'admission.no.prefix', 'ZY', 1, '住院号自动生成前缀'),
(6, '系统名称', 'system.title', '医疗后台管理系统', 1, '前端标题'),
(7, '默认密码', 'system.default.password', '123456', 1, '新建用户与重置密码的默认值'),
(8, '库存预警默认数量', 'stock.warn.default', '100', 0, '入库时的默认预警数量'),
(9, '效期预警天数', 'stock.expire.warn.days', '90', 0, '距离有效期小于该天数时预警'),
(10, '系统超时时间(分钟)', 'system.timeout.minutes', '30', 1, '会话超时时间');

-- ---------------------------------------------------------------------
-- 五、菜单与按钮权限
--    menu_type: M 目录 / C 菜单 / F 按钮
--    component 对应前端 src/views 下的相对路径
-- ---------------------------------------------------------------------
DELETE FROM `sys_menu`;
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `perms`, `icon`, `sort`, `visible`, `status`) VALUES
(1, '首页', 0, 'C', 'index', 'dashboard/index', NULL, 1, 0, NULL, 'HomeFilled', 1, 1, 1),

(10, '系统管理', 0, 'M', 'system', NULL, NULL, 1, 0, NULL, 'Setting', 2, 1, 1),
(101, '用户管理', 10, 'C', 'user', 'system/user/index', NULL, 1, 0, 'system:user:list', 'User', 1, 1, 1),
(102, '角色管理', 10, 'C', 'role', 'system/role/index', NULL, 1, 0, 'system:role:list', 'UserFilled', 2, 1, 1),
(103, '科室管理', 10, 'C', 'dept', 'system/dept/index', NULL, 1, 0, 'system:dept:list', 'OfficeBuilding', 3, 1, 1),
(104, '菜单管理', 10, 'C', 'menu', 'system/menu/index', NULL, 1, 0, 'system:menu:list', 'Menu', 4, 1, 1),
(105, '字典管理', 10, 'C', 'dict', 'system/dict/index', NULL, 1, 0, 'system:dict:list', 'Collection', 5, 1, 1),
(106, '参数设置', 10, 'C', 'config', 'system/config/index', NULL, 1, 0, 'system:config:list', 'Tools', 6, 1, 1),
(107, '操作日志', 10, 'C', 'operation-log', 'system/log/operation', NULL, 1, 0, 'system:log:list', 'Document', 7, 1, 1),
(108, '登录日志', 10, 'C', 'login-log', 'system/log/login', NULL, 1, 0, 'system:log:loginList', 'Clock', 8, 1, 1),

(20, '患者管理', 0, 'M', 'patient', NULL, NULL, 1, 0, NULL, 'User', 3, 1, 1),
(201, '患者档案', 20, 'C', 'archive', 'patient/index', NULL, 1, 0, 'patient:list', 'Notebook', 1, 1, 1),
(202, '就诊卡管理', 20, 'C', 'card', 'patient/card', NULL, 1, 0, 'patient:card:list', 'CreditCard', 2, 1, 1),
(203, '病历管理', 20, 'C', 'record', 'patient/record', NULL, 1, 0, 'patient:record:list', 'Tickets', 3, 1, 1),
(204, '随访管理', 20, 'C', 'followup', 'patient/followup', NULL, 1, 0, 'patient:followup:list', 'ChatDotSquare', 4, 1, 1),

(30, '挂号预约', 0, 'M', 'registration', NULL, NULL, 1, 0, NULL, 'Calendar', 4, 1, 1),
(301, '排班管理', 30, 'C', 'schedule', 'registration/schedule', NULL, 1, 0, 'registration:schedule:list', 'Calendar', 1, 1, 1),
(302, '挂号管理', 30, 'C', 'index', 'registration/index', NULL, 1, 0, 'registration:list', 'Clipboard', 2, 1, 1),
(303, '预约管理', 30, 'C', 'appointment', 'registration/appointment', NULL, 1, 0, 'appointment:list', 'AlarmClock', 3, 1, 1),
(304, '叫号大屏', 30, 'C', 'queue', 'registration/queue', NULL, 1, 0, 'registration:queue:list', 'Microphone', 4, 1, 1),

(40, '门诊管理', 0, 'M', 'clinic', NULL, NULL, 1, 0, NULL, 'FirstAidKit', 5, 1, 1),
(401, '医生工作台', 40, 'C', 'visit', 'clinic/visit', NULL, 1, 0, 'clinic:visit:list', 'Stethoscope', 1, 1, 1),
(402, '处方管理', 40, 'C', 'prescription', 'clinic/prescription', NULL, 1, 0, 'clinic:prescription:list', 'FirstAidKit', 2, 1, 1),
(403, '门诊病历', 40, 'C', 'record', 'clinic/record', NULL, 1, 0, 'clinic:record:list', 'Document', 3, 1, 1),

(50, '住院管理', 0, 'M', 'inpatient', NULL, NULL, 1, 0, NULL, 'OfficeBuilding', 6, 1, 1),
(501, '床位管理', 50, 'C', 'bed', 'inpatient/bed', NULL, 1, 0, 'inpatient:bed:list', 'Grid', 1, 1, 1),
(502, '住院登记', 50, 'C', 'admission', 'inpatient/admission', NULL, 1, 0, 'inpatient:admission:list', 'Tickets', 2, 1, 1),
(503, '医嘱管理', 50, 'C', 'order', 'inpatient/order', NULL, 1, 0, 'inpatient:order:list', 'List', 3, 1, 1),
(504, '护理记录', 50, 'C', 'nursing', 'inpatient/nursing', NULL, 1, 0, 'inpatient:nursing:list', 'FirstAidKit', 4, 1, 1),
(505, '出院结算', 50, 'C', 'settlement', 'inpatient/settlement', NULL, 1, 0, 'inpatient:settlement:list', 'Money', 5, 1, 1),

(60, '药房管理', 0, 'M', 'drug', NULL, NULL, 1, 0, NULL, 'Goods', 7, 1, 1),
(601, '药品目录', 60, 'C', 'index', 'drug/index', NULL, 1, 0, 'drug:list', 'Goods', 1, 1, 1),
(602, '库存管理', 60, 'C', 'stock', 'drug/stock', NULL, 1, 0, 'drug:stock:list', 'Box', 2, 1, 1),
(603, '库房管理', 60, 'C', 'warehouse', 'drug/warehouse', NULL, 1, 0, 'drug:warehouse:list', 'School', 3, 1, 1),
(604, '发药退药', 60, 'C', 'pharmacy', 'drug/pharmacy', NULL, 1, 0, 'drug:pharmacy:list', 'Van', 4, 1, 1),

(70, '检查检验', 0, 'M', 'exam', NULL, NULL, 1, 0, NULL, 'Microscope', 8, 1, 1),
(701, '检查申请', 70, 'C', 'apply', 'exam/apply', NULL, 1, 0, 'exam:apply:list', 'Document', 1, 1, 1),
(702, '检验报告', 70, 'C', 'report', 'exam/report', NULL, 1, 0, 'exam:report:list', 'DataAnalysis', 2, 1, 1),
(703, '危急值管理', 70, 'C', 'critical', 'exam/critical', NULL, 1, 0, 'exam:critical:list', 'Warning', 3, 1, 1),

(80, '收费财务', 0, 'M', 'charge', NULL, NULL, 1, 0, NULL, 'Money', 9, 1, 1),
(801, '收费管理', 80, 'C', 'index', 'charge/index', NULL, 1, 0, 'charge:list', 'Wallet', 1, 1, 1),
(802, '退费管理', 80, 'C', 'refund', 'charge/refund', NULL, 1, 0, 'charge:refund:list', 'RefreshLeft', 2, 1, 1),
(803, '发票管理', 80, 'C', 'invoice', 'charge/invoice', NULL, 1, 0, 'charge:invoice:list', 'Tickets', 3, 1, 1),
(804, '诊疗项目', 80, 'C', 'fee-item', 'charge/fee-item', NULL, 1, 0, 'charge:feeItem:list', 'PriceTag', 4, 1, 1),
(805, '日结月结', 80, 'C', 'finance', 'charge/finance', NULL, 1, 0, 'charge:finance:daily', 'Calendar', 5, 1, 1),

(90, '公共卫生', 0, 'M', 'health', NULL, NULL, 1, 0, NULL, 'Sunny', 10, 1, 1),
(901, '健康档案', 90, 'C', 'record', 'health/record', NULL, 1, 0, 'health:record:list', 'Files', 1, 1, 1),
(902, '疫苗接种', 90, 'C', 'vaccination', 'health/vaccination', NULL, 1, 0, 'health:vaccination:list', 'FirstAidKit', 2, 1, 1),
(903, '传染病上报', 90, 'C', 'infectious', 'health/infectious', NULL, 1, 0, 'health:infectious:list', 'Warning', 3, 1, 1),
(904, '体检管理', 90, 'C', 'checkup', 'health/checkup', NULL, 1, 0, 'health:checkup:list', 'Checked', 4, 1, 1),

(100, '统计报表', 0, 'M', 'report', NULL, NULL, 1, 0, NULL, 'TrendCharts', 11, 1, 1),
(1001, '门诊统计', 100, 'C', 'outpatient', 'report/outpatient', NULL, 1, 0, 'report:outpatient', 'DataLine', 1, 1, 1),
(1002, '住院统计', 100, 'C', 'inpatient', 'report/inpatient', NULL, 1, 0, 'report:inpatient', 'Histogram', 2, 1, 1),
(1003, '收入统计', 100, 'C', 'income', 'report/income', NULL, 1, 0, 'report:income', 'TrendCharts', 3, 1, 1),
(1004, '药品统计', 100, 'C', 'drug', 'report/drug', NULL, 1, 0, 'report:drug', 'PieChart', 4, 1, 1),
(1005, '患者分析', 100, 'C', 'patient', 'report/patient', NULL, 1, 0, 'report:patient', 'PieChart', 5, 1, 1),

(110, '消息通知', 0, 'M', 'message', NULL, NULL, 1, 0, NULL, 'Bell', 12, 1, 1),
(1101, '站内消息', 110, 'C', 'index', 'message/index', NULL, 1, 0, 'message:list', 'Message', 1, 1, 1),
(1102, '公告管理', 110, 'C', 'notice', 'message/notice', NULL, 1, 0, 'message:notice:list', 'Promotion', 2, 1, 1),
(1103, '短信记录', 110, 'C', 'sms', 'message/sms', NULL, 1, 0, 'message:sms:list', 'ChatSquare', 3, 1, 1),

(120, '数据安全', 0, 'M', 'audit', NULL, NULL, 1, 0, NULL, 'Lock', 13, 1, 1),
(1201, '操作审计', 120, 'C', 'operation', 'audit/operation', NULL, 1, 0, 'audit:operation:list', 'View', 1, 1, 1),
(1202, '数据备份', 120, 'C', 'backup', 'audit/backup', NULL, 1, 0, 'audit:backup:list', 'FolderOpened', 2, 1, 1),
(1203, '安全配置', 120, 'C', 'security', 'audit/security', NULL, 1, 0, 'audit:security:view', 'Key', 3, 1, 1);

-- 按钮权限
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`) VALUES
(10101, '用户新增', 101, 'F', NULL, NULL, 'system:user:add', NULL, 1, 1, 1),
(10102, '用户修改', 101, 'F', NULL, NULL, 'system:user:edit', NULL, 2, 1, 1),
(10103, '用户删除', 101, 'F', NULL, NULL, 'system:user:remove', NULL, 3, 1, 1),
(10104, '重置密码', 101, 'F', NULL, NULL, 'system:user:resetPwd', NULL, 4, 1, 1),
(10105, '用户状态', 101, 'F', NULL, NULL, 'system:user:status', NULL, 5, 1, 1),
(10201, '角色新增', 102, 'F', NULL, NULL, 'system:role:add', NULL, 1, 1, 1),
(10202, '角色修改', 102, 'F', NULL, NULL, 'system:role:edit', NULL, 2, 1, 1),
(10203, '角色删除', 102, 'F', NULL, NULL, 'system:role:remove', NULL, 3, 1, 1),
(10204, '分配权限', 102, 'F', NULL, NULL, 'system:role:permission', NULL, 4, 1, 1),
(10301, '科室新增', 103, 'F', NULL, NULL, 'system:dept:add', NULL, 1, 1, 1),
(10302, '科室修改', 103, 'F', NULL, NULL, 'system:dept:edit', NULL, 2, 1, 1),
(10303, '科室删除', 103, 'F', NULL, NULL, 'system:dept:remove', NULL, 3, 1, 1),
(10401, '菜单新增', 104, 'F', NULL, NULL, 'system:menu:add', NULL, 1, 1, 1),
(10402, '菜单修改', 104, 'F', NULL, NULL, 'system:menu:edit', NULL, 2, 1, 1),
(10403, '菜单删除', 104, 'F', NULL, NULL, 'system:menu:remove', NULL, 3, 1, 1),
(10501, '字典新增', 105, 'F', NULL, NULL, 'system:dict:add', NULL, 1, 1, 1),
(10502, '字典修改', 105, 'F', NULL, NULL, 'system:dict:edit', NULL, 2, 1, 1),
(10503, '字典删除', 105, 'F', NULL, NULL, 'system:dict:remove', NULL, 3, 1, 1),
(10601, '参数新增', 106, 'F', NULL, NULL, 'system:config:add', NULL, 1, 1, 1),
(10602, '参数修改', 106, 'F', NULL, NULL, 'system:config:edit', NULL, 2, 1, 1),
(10603, '参数删除', 106, 'F', NULL, NULL, 'system:config:remove', NULL, 3, 1, 1),
(20101, '患者新增', 201, 'F', NULL, NULL, 'patient:add', NULL, 1, 1, 1),
(20102, '患者修改', 201, 'F', NULL, NULL, 'patient:edit', NULL, 2, 1, 1),
(20103, '患者删除', 201, 'F', NULL, NULL, 'patient:remove', NULL, 3, 1, 1),
(20201, '发卡', 202, 'F', NULL, NULL, 'patient:card:add', NULL, 1, 1, 1),
(20202, '挂失', 202, 'F', NULL, NULL, 'patient:card:loss', NULL, 2, 1, 1),
(20203, '补卡', 202, 'F', NULL, NULL, 'patient:card:reissue', NULL, 3, 1, 1),
(20204, '注销', 202, 'F', NULL, NULL, 'patient:card:cancel', NULL, 4, 1, 1),
(20401, '随访新增', 204, 'F', NULL, NULL, 'patient:followup:add', NULL, 1, 1, 1),
(20402, '随访修改', 204, 'F', NULL, NULL, 'patient:followup:edit', NULL, 2, 1, 1),
(30101, '排班新增', 301, 'F', NULL, NULL, 'registration:schedule:add', NULL, 1, 1, 1),
(30102, '排班修改', 301, 'F', NULL, NULL, 'registration:schedule:edit', NULL, 2, 1, 1),
(30103, '停诊', 301, 'F', NULL, NULL, 'registration:schedule:stop', NULL, 3, 1, 1),
(30201, '挂号', 302, 'F', NULL, NULL, 'registration:add', NULL, 1, 1, 1),
(30202, '退号', 302, 'F', NULL, NULL, 'registration:cancel', NULL, 2, 1, 1),
(30203, '叫号', 302, 'F', NULL, NULL, 'registration:call', NULL, 3, 1, 1),
(30204, '患者查询', 302, 'F', NULL, NULL, 'patient:list', NULL, 4, 1, 1),
(30301, '预约新增', 303, 'F', NULL, NULL, 'appointment:add', NULL, 1, 1, 1),
(30302, '取消预约', 303, 'F', NULL, NULL, 'appointment:cancel', NULL, 2, 1, 1),
(40101, '接诊', 401, 'F', NULL, NULL, 'clinic:visit:receive', NULL, 1, 1, 1),
(40102, '保存病历', 401, 'F', NULL, NULL, 'clinic:record:save', NULL, 2, 1, 1),
(40201, '开处方', 402, 'F', NULL, NULL, 'clinic:prescription:add', NULL, 1, 1, 1),
(40203, '作废处方', 402, 'F', NULL, NULL, 'clinic:prescription:remove', NULL, 3, 1, 1),
(50101, '床位新增', 501, 'F', NULL, NULL, 'inpatient:bed:add', NULL, 1, 1, 1),
(50102, '床位修改', 501, 'F', NULL, NULL, 'inpatient:bed:edit', NULL, 2, 1, 1),
(50201, '入院登记', 502, 'F', NULL, NULL, 'inpatient:admission:add', NULL, 1, 1, 1),
(50202, '出院办理', 502, 'F', NULL, NULL, 'inpatient:admission:discharge', NULL, 2, 1, 1),
(50203, '转科', 502, 'F', NULL, NULL, 'inpatient:admission:transfer', NULL, 3, 1, 1),
(50204, '床位查询', 502, 'F', NULL, NULL, 'inpatient:bed:list', NULL, 4, 1, 1),
(50301, '开医嘱', 503, 'F', NULL, NULL, 'inpatient:order:add', NULL, 1, 1, 1),
(50302, '执行医嘱', 503, 'F', NULL, NULL, 'inpatient:order:execute', NULL, 2, 1, 1),
(50303, '停止医嘱', 503, 'F', NULL, NULL, 'inpatient:order:stop', NULL, 3, 1, 1),
(50401, '护理记录新增', 504, 'F', NULL, NULL, 'inpatient:nursing:add', NULL, 1, 1, 1),
(60101, '药品新增', 601, 'F', NULL, NULL, 'drug:add', NULL, 1, 1, 1),
(60102, '药品修改', 601, 'F', NULL, NULL, 'drug:edit', NULL, 2, 1, 1),
(60103, '药品删除', 601, 'F', NULL, NULL, 'drug:remove', NULL, 3, 1, 1),
(60201, '药品入库', 602, 'F', NULL, NULL, 'drug:stock:in', NULL, 1, 1, 1),
(60202, '药品出库', 602, 'F', NULL, NULL, 'drug:stock:out', NULL, 2, 1, 1),
(60401, '确认发药', 604, 'F', NULL, NULL, 'drug:pharmacy:dispense', NULL, 1, 1, 1),
(60402, '退药', 604, 'F', NULL, NULL, 'drug:pharmacy:return', NULL, 2, 1, 1),
(60403, '处方查询', 604, 'F', NULL, NULL, 'clinic:prescription:list', NULL, 3, 1, 1),
(70101, '开检查单', 701, 'F', NULL, NULL, 'exam:apply:add', NULL, 1, 1, 1),
(70102, '样本采集', 701, 'F', NULL, NULL, 'exam:apply:sample', NULL, 2, 1, 1),
(70103, '诊疗项目查询', 701, 'F', NULL, NULL, 'charge:feeItem:list', NULL, 3, 1, 1),
(70201, '录入结果', 702, 'F', NULL, NULL, 'exam:report:add', NULL, 1, 1, 1),
(70202, '报告审核', 702, 'F', NULL, NULL, 'exam:report:audit', NULL, 2, 1, 1),
(70203, '报告发布', 702, 'F', NULL, NULL, 'exam:report:publish', NULL, 3, 1, 1),
(70301, '危急值处理', 703, 'F', NULL, NULL, 'exam:critical:handle', NULL, 1, 1, 1),
(80101, '收费', 801, 'F', NULL, NULL, 'charge:add', NULL, 1, 1, 1),
(80102, '退费', 801, 'F', NULL, NULL, 'charge:refund', NULL, 2, 1, 1),
(80103, '处方查看', 801, 'F', NULL, NULL, 'clinic:prescription:list', NULL, 3, 1, 1),
(80301, '发票打印', 803, 'F', NULL, NULL, 'charge:invoice:print', NULL, 1, 1, 1),
(80302, '发票作废', 803, 'F', NULL, NULL, 'charge:invoice:void', NULL, 2, 1, 1),
(80401, '诊疗项目维护', 804, 'F', NULL, NULL, 'charge:feeItem:edit', NULL, 1, 1, 1),
(90101, '档案新增', 901, 'F', NULL, NULL, 'health:record:add', NULL, 1, 1, 1),
(90102, '档案修改', 901, 'F', NULL, NULL, 'health:record:edit', NULL, 2, 1, 1),
(90201, '接种记录新增', 902, 'F', NULL, NULL, 'health:vaccination:add', NULL, 1, 1, 1),
(90301, '传染病上报', 903, 'F', NULL, NULL, 'health:infectious:add', NULL, 1, 1, 1),
(90302, '报卡审核', 903, 'F', NULL, NULL, 'health:infectious:audit', NULL, 2, 1, 1),
(90401, '体检记录新增', 904, 'F', NULL, NULL, 'health:checkup:add', NULL, 1, 1, 1),
(10001, '报表导出', 100, 'F', NULL, NULL, 'report:export', NULL, 99, 1, 1),
(11011, '消息已读', 1101, 'F', NULL, NULL, 'message:read', NULL, 1, 1, 1),
(11021, '公告发布', 1102, 'F', NULL, NULL, 'message:notice:add', NULL, 1, 1, 1),
(11022, '公告删除', 1102, 'F', NULL, NULL, 'message:notice:remove', NULL, 2, 1, 1),
(11031, '发送短信', 1103, 'F', NULL, NULL, 'message:sms:send', NULL, 1, 1, 1),
(12021, '手动备份', 1202, 'F', NULL, NULL, 'audit:backup:add', NULL, 1, 1, 1),
(12022, '数据恢复', 1202, 'F', NULL, NULL, 'audit:backup:restore', NULL, 2, 1, 1),
(12023, '备份删除', 1202, 'F', NULL, NULL, 'audit:backup:remove', NULL, 3, 1, 1),
(12031, '安全配置修改', 1203, 'F', NULL, NULL, 'audit:security:edit', NULL, 1, 1, 1);

-- 超级管理员拥有全部菜单权限
DELETE FROM `sys_role_menu`;
SET @row := 0;
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 1, m.`id` FROM `sys_menu` m ORDER BY m.`id`;

-- 系统管理员：系统管理 + 数据安全 + 全部只读
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 2, m.`id` FROM `sys_menu` m
WHERE m.`id` IN (10, 101, 10101, 10102, 10103, 10104, 10105, 102, 10201, 10202, 10203, 10204,
                 103, 10301, 10302, 10303, 104, 10401, 10402, 10403, 105, 10501, 10502, 10503,
                 106, 10601, 10602, 10603, 107, 108, 120, 1201, 1202, 12021, 12022, 12023, 1203, 12031);

-- 医生：患者、挂号、门诊、检查检验、住院登记
-- 说明：检查检验(70) 是 701/70101 等已授权菜单的父级容器，补上才能让侧边栏正确归组；
--       住院登记(50→502→50201) 是"建议住院"入口——医生接诊后判断需住院可直接登记入院，
--       不再需要先退出门诊去住院模块找权限。
--       30204(patient:list) 是住院登记页拉患者下拉所必需的查询权限（同收费员一致）。
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 3, m.`id` FROM `sys_menu` m
WHERE m.`id` IN (20, 201, 20101, 20102, 202, 203, 204, 30, 302, 30201, 30203, 30204, 303, 304,
                 40, 401, 40101, 40102, 402, 40201, 403,
                 70, 701, 70101, 70103, 702, 70201, 70202,
                 50, 502, 50201, 50204);

-- 护士：患者、住院、护理
-- 50204(inpatient:bed:list) 之外补 30204(patient:list)：住院登记页拉患者下拉同样依赖它，
-- 此前护士能进登记页却选不了患者，这里一并补齐。
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 4, m.`id` FROM `sys_menu` m
WHERE m.`id` IN (20, 201, 20101, 20102, 203, 204, 20401, 50, 502, 50204, 30204, 503, 50301, 50302, 504, 50401);

-- 收费员：挂号、收费
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 5, m.`id` FROM `sys_menu` m
WHERE m.`id` IN (30, 302, 30201, 30202, 30204, 80, 801, 80101, 80102, 80103, 802, 803, 80301, 804);

-- 药师：药房
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 6, m.`id` FROM `sys_menu` m
WHERE m.`id` IN (60, 601, 60101, 60102, 60103, 602, 60201, 60202, 603, 604, 60401, 60402, 60403);

-- 统计员：报表
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT @row := @row + 1, 7, m.`id` FROM `sys_menu` m
WHERE m.`id` IN (100, 1001, 1002, 1003, 1004, 1005, 10001);

-- ---------------------------------------------------------------------
-- 六、数据字典
-- ---------------------------------------------------------------------
DELETE FROM `sys_dict_type`;
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`) VALUES
(1, '性别', 'sys_gender', 1, '系统通用'),
(2, '启用状态', 'sys_status', 1, '系统通用'),
(3, '血型', 'blood_type', 1, '患者管理'),
(4, '号别', 'reg_type', 1, '挂号预约'),
(5, '就诊时段', 'time_period', 1, '挂号预约'),
(6, '科室类型', 'dept_type', 1, '系统管理'),
(7, '就诊类型', 'visit_type', 1, '病历管理'),
(8, '收费类型', 'charge_type', 1, '收费财务'),
(9, '支付方式', 'pay_type', 1, '收费财务'),
(10, '消息类型', 'msg_type', 1, '消息通知'),
(11, '检查类型', 'exam_type', 1, '检查检验'),
(12, '诊疗项目类型', 'fee_item_type', 1, '收费财务'),
(13, '病历状态', 'record_status', 1, '门诊管理'),
(14, '随访方式', 'follow_type', 1, '患者管理'),
(15, '床位类型', 'bed_type', 1, '住院管理'),
(16, '床位状态', 'bed_status', 1, '住院管理'),
(17, '医嘱类型', 'order_type', 1, '住院管理'),
(18, '医保类型', 'insurance_type', 1, '住院管理'),
(19, '处方类型', 'presc_type', 1, '门诊管理'),
(20, '职称', 'job_title', 1, '系统通用'),
(21, '学历', 'education', 1, '系统通用'),
(22, '常见诊断', 'diagnosis', 1, '门诊管理'),
(23, '疫苗名称', 'vaccine_name', 1, '公共卫生'),
(24, '标本类型', 'sample_type', 1, '检查检验'),
(25, '公告类型', 'notice_type', 1, '消息通知');

DELETE FROM `sys_dict_item`;
INSERT INTO `sys_dict_item` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort`, `is_default`, `status`, `remark`) VALUES
(1, 'sys_gender', '女', '0', 1, 1, 1, NULL),
(2, 'sys_gender', '男', '1', 2, 0, 1, NULL),
(3, 'sys_status', '禁用', '0', 1, 0, 1, NULL),
(4, 'sys_status', '启用', '1', 2, 1, 1, NULL),
(5, 'blood_type', 'A型', 'A', 1, 0, 1, NULL),
(6, 'blood_type', 'B型', 'B', 2, 0, 1, NULL),
(7, 'blood_type', 'O型', 'O', 3, 0, 1, NULL),
(8, 'blood_type', 'AB型', 'AB', 4, 0, 1, NULL),
(9, 'blood_type', '其他', 'OTHER', 5, 0, 1, NULL),
(10, 'reg_type', '普通号', '1', 1, 1, 1, NULL),
(11, 'reg_type', '专家号', '2', 2, 0, 1, NULL),
(12, 'reg_type', '急诊号', '3', 3, 0, 1, NULL),
(13, 'time_period', '上午', '1', 1, 1, 1, NULL),
(14, 'time_period', '下午', '2', 2, 0, 1, NULL),
(15, 'time_period', '晚上', '3', 3, 0, 1, NULL),
(16, 'dept_type', '门诊科室', '1', 1, 0, 1, NULL),
(17, 'dept_type', '住院科室', '2', 2, 0, 1, NULL),
(18, 'dept_type', '医技科室', '3', 3, 0, 1, NULL),
(19, 'dept_type', '行政科室', '4', 4, 0, 1, NULL),
(20, 'dept_type', '药房', '5', 5, 0, 1, NULL),
(21, 'visit_type', '门诊', '1', 1, 1, 1, NULL),
(22, 'visit_type', '住院', '2', 2, 0, 1, NULL),
(23, 'charge_type', '挂号费', '1', 1, 0, 1, NULL),
(24, 'charge_type', '门诊费', '2', 2, 0, 1, NULL),
(25, 'charge_type', '住院费', '3', 3, 0, 1, NULL),
(26, 'pay_type', '现金', '1', 1, 0, 1, NULL),
(27, 'pay_type', '微信', '2', 2, 0, 1, NULL),
(28, 'pay_type', '支付宝', '3', 3, 0, 1, NULL),
(29, 'pay_type', '医保', '4', 4, 0, 1, NULL),
(30, 'pay_type', '银行卡', '5', 5, 0, 1, NULL),
(31, 'msg_type', '系统通知', '1', 1, 1, 1, NULL),
(32, 'msg_type', '待办提醒', '2', 2, 0, 1, NULL),
(33, 'msg_type', '预警提醒', '3', 3, 0, 1, NULL),
(34, 'exam_type', '检验', '1', 1, 1, 1, NULL),
(35, 'exam_type', '检查（影像）', '2', 2, 0, 1, NULL),
(36, 'fee_item_type', '药品', '1', 1, 0, 1, NULL),
(37, 'fee_item_type', '诊疗', '2', 2, 0, 1, NULL),
(38, 'fee_item_type', '检查', '3', 3, 0, 1, NULL),
(39, 'fee_item_type', '床位', '4', 4, 0, 1, NULL),
(40, 'fee_item_type', '护理', '5', 5, 0, 1, NULL),
(41, 'record_status', '草稿', '0', 1, 1, 1, NULL),
(42, 'record_status', '已提交', '1', 2, 0, 1, NULL),
(43, 'record_status', '已归档', '2', 3, 0, 1, NULL),
(44, 'follow_type', '电话随访', '1', 1, 1, 1, NULL),
(45, 'follow_type', '上门随访', '2', 2, 0, 1, NULL),
(46, 'follow_type', '短信随访', '3', 3, 0, 1, NULL),
(47, 'bed_type', '普通床', '1', 1, 1, 1, NULL),
(48, 'bed_type', '重症床', '2', 2, 0, 1, NULL),
(49, 'bed_type', 'VIP床', '3', 3, 0, 1, NULL),
(50, 'bed_status', '空闲', '0', 1, 0, 1, NULL),
(51, 'bed_status', '占用', '1', 2, 0, 1, NULL),
(52, 'bed_status', '维修', '2', 3, 0, 1, NULL),
(53, 'order_type', '长期医嘱', '1', 1, 1, 1, NULL),
(54, 'order_type', '临时医嘱', '2', 2, 0, 1, NULL),
(55, 'insurance_type', '自费', '1', 1, 0, 1, NULL),
(56, 'insurance_type', '职工医保', '2', 2, 0, 1, NULL),
(57, 'insurance_type', '居民医保', '3', 3, 0, 1, NULL),
(58, 'presc_type', '西药处方', '1', 1, 1, 1, NULL),
(59, 'presc_type', '中药处方', '2', 2, 0, 1, NULL),
(60, 'presc_type', '中成药处方', '3', 3, 0, 1, NULL),
(61, 'job_title', '住院医师', '1', 1, 0, 1, NULL),
(62, 'job_title', '主治医师', '2', 2, 0, 1, NULL),
(63, 'job_title', '副主任医师', '3', 3, 0, 1, NULL),
(64, 'job_title', '主任医师', '4', 4, 0, 1, NULL),
(65, 'education', '高中及以下', '1', 1, 0, 1, NULL),
(66, 'education', '大专', '2', 2, 0, 1, NULL),
(67, 'education', '本科', '3', 3, 0, 1, NULL),
(68, 'education', '硕士及以上', '4', 4, 0, 1, NULL),
(69, 'diagnosis', '上呼吸道感染', 'J06', 1, 0, 1, 'J06 上呼吸道感染'),
(70, 'diagnosis', '急性支气管炎', 'J20', 2, 0, 1, 'J20 急性支气管炎'),
(71, 'diagnosis', '社区获得性肺炎', 'J18', 3, 0, 1, 'J18 肺炎'),
(72, 'diagnosis', '高血压病', 'I10', 4, 0, 1, 'I10 原发性高血压'),
(73, 'diagnosis', '2型糖尿病', 'E11', 5, 0, 1, 'E11 2型糖尿病'),
(74, 'diagnosis', '急性胃炎', 'K29', 6, 0, 1, 'K29 急性胃炎'),
(75, 'diagnosis', '慢性胃炎', 'K29.3', 7, 0, 1, 'K29.3 慢性胃炎'),
(76, 'diagnosis', '普通感冒', 'J00', 8, 0, 1, 'J00 急性上呼吸道感染'),
(77, 'vaccine_name', '新冠疫苗', 'COVID', 1, 0, 1, NULL),
(78, 'vaccine_name', '流感疫苗', 'FLU', 2, 0, 1, NULL),
(79, 'vaccine_name', '乙肝疫苗', 'HBV', 3, 0, 1, NULL),
(80, 'vaccine_name', '卡介苗', 'BCG', 4, 0, 1, NULL),
(81, 'vaccine_name', '脊灰疫苗', 'IPV', 5, 0, 1, NULL),
(82, 'sample_type', '静脉血', '1', 1, 1, 1, NULL),
(83, 'sample_type', '动脉血', '2', 2, 0, 1, NULL),
(84, 'sample_type', '尿', '3', 3, 0, 1, NULL),
(85, 'sample_type', '痰', '4', 4, 0, 1, NULL),
(86, 'sample_type', '粪便', '5', 5, 0, 1, NULL),
(87, 'notice_type', '院内公告', '1', 1, 1, 1, NULL),
(88, 'notice_type', '科室公告', '2', 2, 0, 1, NULL);

-- ---------------------------------------------------------------------
-- 七、诊疗项目价格
-- ---------------------------------------------------------------------
DELETE FROM `biz_fee_item`;
INSERT INTO `biz_fee_item` (`id`, `item_code`, `item_name`, `item_type`, `unit`, `price`, `medical_insurance`, `status`) VALUES
(1, 'EXAM001', '血常规', 3, '次', 30.00, 1, 1),
(2, 'EXAM002', '尿常规', 3, '次', 20.00, 1, 1),
(3, 'EXAM003', '肝功能检查', 3, '次', 60.00, 1, 1),
(4, 'EXAM004', '肾功能检查', 3, '次', 55.00, 1, 1),
(5, 'EXAM005', '血糖测定', 3, '次', 15.00, 1, 1),
(6, 'EXAM006', '心电图', 3, '次', 40.00, 1, 1),
(7, 'EXAM007', '胸部X线片', 3, '次', 80.00, 1, 1),
(8, 'EXAM008', '腹部超声', 3, '次', 120.00, 1, 1),
(9, 'TREAT001', '普通门诊诊查费', 2, '次', 15.00, 1, 1),
(10, 'TREAT002', '专家门诊诊查费', 2, '次', 50.00, 1, 1),
(11, 'TREAT001', '急诊诊查费', 2, '次', 30.00, 1, 1),
(12, 'TREAT002', '静脉输液', 2, '次', 20.00, 1, 1),
(13, 'TREAT003', '肌肉注射', 2, '次', 8.00, 1, 1),
(14, 'NURS001', '一级护理', 5, '天', 40.00, 1, 1),
(15, 'NURS002', '二级护理', 5, '天', 25.00, 1, 1),
(16, 'BED001', '普通床位费', 4, '天', 40.00, 1, 1),
(17, 'BED002', '重症床位费', 4, '天', 120.00, 1, 1),
(18, 'BED003', 'VIP床位费', 4, '天', 300.00, 0, 1),
(19, 'REG001', '普通挂号费', 2, '次', 10.00, 1, 1),
(20, 'REG002', '专家挂号费', 2, '次', 50.00, 1, 1);

-- ---------------------------------------------------------------------
-- 八、库房与药品
-- ---------------------------------------------------------------------
DELETE FROM `biz_warehouse`;
INSERT INTO `biz_warehouse` (`id`, `warehouse_code`, `warehouse_name`, `dept_id`, `warehouse_type`, `manager`, `phone`, `address`, `status`) VALUES
(1, 'WH001', '门诊药房', 10, 1, '何药师', '13800000015', '门诊楼 1 层', 1),
(2, 'WH002', '中心库房', 10, 3, '吴主管', '13800000017', '住院楼 -1 层', 1);

DELETE FROM `biz_drug`;
INSERT INTO `biz_drug` (`id`, `drug_code`, `drug_name`, `generic_name`, `spec`, `dosage_form`, `unit`, `manufacturer`, `approval_no`, `purchase_price`, `sale_price`, `category`, `is_special`, `status`) VALUES
(1, 'D001', '阿莫西林胶囊', '阿莫西林', '0.25g*24粒', '胶囊', '盒', '华北制药', '国药准字H13020726', 2.00, 2.50, '抗感染药', 0, 1),
(2, 'D002', '布洛芬缓释胶囊', '布洛芬', '0.2g*20粒', '胶囊', '盒', '中美天津史克', '国药准字H12020224', 12.00, 15.00, '解热镇痛药', 0, 1),
(3, 'D003', '头孢克肟分散片', '头孢克肟', '0.1g*12片', '片剂', '盒', '广州白云山', '国药准字H20040738', 18.00, 24.00, '抗感染药', 0, 1),
(4, 'D004', '氯化钠注射液', '氯化钠', '0.9% 500ml', '注射液', '瓶', '四川科伦', '国药准字H20043871', 3.50, 5.00, '输液制剂', 0, 1),
(5, 'D005', '葡萄糖注射液', '葡萄糖', '5% 500ml', '注射液', '瓶', '四川科伦', '国药准字H20043873', 3.20, 4.50, '输液制剂', 0, 1),
(6, 'D006', '阿托伐他汀钙片', '阿托伐他汀', '20mg*7片', '片剂', '盒', '辉瑞制药', '国药准字H20051408', 28.00, 35.00, '心血管用药', 0, 1),
(7, 'D007', '二甲双胍缓释片', '二甲双胍', '0.5g*30片', '片剂', '盒', '中美上海施贵宝', '国药准字H20040664', 22.00, 29.00, '糖尿病用药', 0, 1),
(8, 'D008', '感冒灵颗粒', '复方感冒灵', '10g*9袋', '颗粒剂', '盒', '广州白云山', '国药准字Z44020038', 9.00, 13.00, '感冒用药', 0, 1),
(9, 'D009', '维生素C片', '维生素C', '0.1g*100片', '片剂', '瓶', '东北制药', '国药准字H21020713', 5.00, 8.00, '维生素类', 0, 1),
(10, 'D010', '蒙脱石散', '蒙脱石', '3g*10袋', '散剂', '盒', '博福-益普生天津', '国药准字H20000608', 14.00, 19.00, '消化系统用药', 0, 1),
(11, 'D011', '注射用头孢曲松钠', '头孢曲松', '1g*5支', '注射剂', '盒', '上海罗氏', '国药准字H20033187', 45.00, 58.00, '抗感染药', 0, 1),
(12, 'D012', '复方甘草片', '复方甘草', '100片', '片剂', '瓶', '哈药六厂', '国药准字H23020391', 6.00, 9.00, '呼吸系统用药', 0, 1);

DELETE FROM `biz_stock`;
INSERT INTO `biz_stock` (`id`, `drug_id`, `batch_no`, `quantity`, `lock_quantity`, `warn_num`, `produce_date`, `expire_date`, `warehouse_id`) VALUES
(1, 1, 'A20250101', 500, 0, 100, '2025-01-01', '2027-01-01', 1),
(2, 2, 'A20250201', 80, 0, 100, '2025-02-01', '2027-02-01', 1),
(3, 3, 'A20250301', 300, 0, 100, '2025-03-01', '2027-03-01', 1),
(4, 4, 'B20250101', 400, 0, 100, '2025-01-10', '2028-01-10', 2),
(5, 5, 'B20250210', 350, 0, 100, '2025-02-10', '2028-02-10', 2),
(6, 6, 'C20250115', 200, 0, 100, '2025-01-15', '2027-01-15', 1),
(7, 7, 'C20250215', 260, 0, 100, '2025-02-15', '2027-02-15', 1),
(8, 8, 'D20250401', 420, 0, 100, '2025-04-01', '2027-04-01', 1),
(9, 9, 'D20250120', 150, 0, 100, '2025-01-20', '2028-01-20', 1),
(10, 10, 'E20250125', 180, 0, 100, '2025-01-25', '2027-01-25', 1),
(11, 11, 'B20250301', 90, 0, 100, '2025-03-01', '2027-03-01', 2),
(12, 12, 'F20250601', 60, 0, 100, '2025-06-01', '2025-12-01', 1);

-- ---------------------------------------------------------------------
-- 九、患者档案（身份证号与手机号为 AES 密文，*_hash 为 SHA-256 摘要）
-- ---------------------------------------------------------------------
DELETE FROM `biz_patient`;
INSERT INTO `biz_patient` (`id`, `patient_no`, `name`, `gender`, `birth_date`, `id_card`, `id_card_hash`, `phone`, `phone_hash`, `address`, `blood_type`, `allergy_history`, `medical_history`, `family_history`, `card_no`, `card_balance`, `tags`, `dept_id`, `status`, `create_by`) VALUES
(1001, 'P0001', '张三', 1, '1990-03-07', 'ghZ5RQpFPThFdWpqLn/NpZBngMrrFLHBdr3DwGZV5Cs=', '726f20c387942a750bdaf25468718e0b32791ce13e728485f739ecfaa6ebeea0', 'GpclX6uRnCgo6tQvIJb+0w==', 'b1c4769e3ad14f68ea1a96b73bbe5a83d90792f044828c5daf9d908b0738b177', '深圳市南山区科技园路 1 号', 'A', '青霉素过敏', '2 年前阑尾炎手术，术后恢复良好。', '父亲有高血压病史。', 'C10001', 100.00, '慢病,VIP', 2, 1, 1),
(1002, 'P0002', '李四', 0, '1985-12-12', 'B6CTCD7IW7aMS8PZmH+e6hcfmgCMiAyXSyysgjG/pRM=', 'dcdeb9c32a71477735b0a953d0c93b8a77f2127420b720c68d8b250e8d88b17b', 'Rq1dcHLI+gIcYc+VLrF2hg==', '412d65b4d8f37676654685a6a525363a8a635ed140a69ac30a30f344ae0dd3b1', '深圳市福田区福华路 8 号', 'B', NULL, '慢性胃炎病史 3 年，规律服药。', '母亲有糖尿病史。', 'C10002', 50.00, '慢病', 2, 1, 1),
(1003, 'P0003', '王五', 1, '1995-07-21', 'h3f3YXeD1a80q0bDEKBN9JpMdJ/BkYFhcgx9sCPnfxg=', 'c929e4b1213fd0b0d0068de96b9125a62e2741808fc86889b3ca19116ca637da', '/U7GlHKcqUKtQ1fh/wlXXw==', '965eac4d7b0698a18e3c59c34023d9cd529752c7e5fd4164209d2e7df010e2ad', '深圳市罗湖区人民南路 200 号', 'O', NULL, NULL, NULL, 'C10003', 0.00, NULL, 3, 1, 1),
(1004, 'P0004', '赵六', 0, '1978-03-15', 'VnuTIuhrmVYMWJuxRBzVODrHo+OtQuP5YjvfgEX30vw=', 'b468a9c0a4632e3f91fd3d2aab0f29e440c01a30f79d300347bd2b2e15f3fb7e', 'CwfMcNY8xgcB7ETXdgzAyQ==', 'd694cdadfdf802a16d1fdd46fa055fec33cd5a3ce2be6ed69b2681fce087da0b', '深圳市宝安区新湖路 66 号', 'AB', '磺胺类药物过敏', '高血压病史 5 年，服用氨氯地平。', '父亲有高血压病史。', 'C10004', 20.00, '慢病,高风险', 2, 1, 1),
(1005, 'P0005', '孙七', 1, '2001-02-03', '3ao0tXaQpl94c/4UgrqAYfb6kyPWIhkUGSAEm613eFg=', '2e343fdce06931b722e2b949ac5ecee8755dd68993e5de57342a38306ee31a2e', 'zML54xPd8wpSCXc3ZNWd0g==', '649210e5028573beab463a2c4463e9563e0707f124c3c3ae62b4042d60ca15e7', '深圳市龙岗区龙城大道 3 号', 'A', NULL, NULL, NULL, 'C10005', 0.00, NULL, 4, 1, 1),
(1006, 'P0006', '周八', 0, '1968-08-12', 'umX2YqBrKtu96p4yW8D+UU+aGtjBSH4xQA2ldOVSZ00=', 'c348eb68f867c22ce7a2974ad8af4c45fe444a16b99a8d98754e9e38ed910c8e', 'tN+iyiG8Gs6MZg6Y+VmYrw==', '871b64ef08d7bfd84d12291c9c572cdd6e64f6852982090cfa3ffc9e62ffcaa5', '深圳市南山区同济路 18 号', 'O', '碘造影剂过敏', '2 型糖尿病 8 年，血糖控制不佳。', '兄长有糖尿病史。', 'C10006', 0.00, '慢病,高风险', 2, 1, 1);

DELETE FROM `biz_patient_card`;
INSERT INTO `biz_patient_card` (`id`, `patient_id`, `card_no`, `card_type`, `balance`, `status`, `issue_time`, `remark`) VALUES
(1, 1001, 'C10001', 2, 100.00, 1, '2025-01-05 09:00:00', '储值卡'),
(2, 1002, 'C10002', 2, 50.00, 1, '2025-01-06 09:30:00', '储值卡'),
(3, 1003, 'C10003', 1, 0.00, 1, '2025-01-06 10:10:00', '普通卡'),
(4, 1004, 'C10004', 1, 20.00, 1, '2025-01-07 08:50:00', '普通卡'),
(5, 1005, 'C10005', 1, 0.00, 1, '2025-01-07 14:20:00', '普通卡'),
(6, 1006, 'C10006', 3, 0.00, 1, '2025-01-08 09:15:00', '医保卡');

-- ---------------------------------------------------------------------
-- 十、床位
-- ---------------------------------------------------------------------
DELETE FROM `biz_bed`;
INSERT INTO `biz_bed` (`id`, `ward_id`, `ward_name`, `dept_id`, `bed_no`, `bed_type`, `price`, `status`, `patient_id`) VALUES
(1, 1, '内科一病区', 2, 'A01', 1, 40.00, 0, NULL),
(2, 1, '内科一病区', 2, 'A02', 1, 40.00, 0, NULL),
(3, 1, '内科一病区', 2, 'A03', 2, 120.00, 0, NULL),
(4, 1, '内科一病区', 2, 'A04', 1, 40.00, 0, NULL),
(5, 1, '内科一病区', 2, 'A05', 1, 40.00, 0, NULL),
(6, 1, '内科一病区', 2, 'A06', 3, 300.00, 0, NULL),
(7, 2, '外科一病区', 3, 'B01', 1, 40.00, 0, NULL),
(8, 2, '外科一病区', 3, 'B02', 1, 40.00, 0, NULL),
(9, 2, '外科一病区', 3, 'B03', 2, 120.00, 0, NULL),
(10, 2, '外科一病区', 3, 'B04', 1, 40.00, 2, NULL),
(11, 2, '外科一病区', 3, 'B05', 1, 40.00, 0, NULL),
(12, 2, '外科一病区', 3, 'B06', 1, 40.00, 0, NULL);

-- ---------------------------------------------------------------------
-- 十一、排班（相对今天生成，始终可用）
-- ---------------------------------------------------------------------
DELETE FROM `biz_schedule`;
INSERT INTO `biz_schedule` (`id`, `doctor_id`, `dept_id`, `schedule_date`, `time_period`, `reg_type`, `total_num`, `remain_num`, `reg_fee`, `status`, `create_by`) VALUES
(1, 3, 2, CURRENT_DATE, 1, 1, 30, 30, 10.00, 1, 1),
(2, 3, 2, CURRENT_DATE, 2, 2, 20, 20, 50.00, 1, 1),
(3, 3, 2, CURRENT_DATE + INTERVAL 1 DAY, 1, 1, 30, 30, 10.00, 1, 1),
(4, 3, 2, CURRENT_DATE + INTERVAL 1 DAY, 2, 2, 20, 20, 50.00, 1, 1),
(5, 3, 3, CURRENT_DATE + INTERVAL 1 DAY, 1, 1, 25, 25, 10.00, 1, 1),
(6, 3, 3, CURRENT_DATE + INTERVAL 2 DAY, 1, 2, 15, 15, 50.00, 1, 1);
