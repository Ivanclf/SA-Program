-- 促销活动管理标准化系统 - 数据库建表脚本
-- 数据库: promotion
USE promotion;

-- ============================================
-- 第一步：删除已有表（按依赖关系逆序删除）
-- ============================================
DROP TABLE IF EXISTS `event_log`;
DROP TABLE IF EXISTS `promotion_sku`;
DROP TABLE IF EXISTS `audit_record`;
DROP TABLE IF EXISTS `promotion`;
DROP TABLE IF EXISTS `sku`;
DROP TABLE IF EXISTS `user`;

-- ============================================
-- 第二步：创建表结构
-- ============================================

-- 1. 用户表（user）
CREATE TABLE IF NOT EXISTS `user` (
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户唯一标识',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '加密密码',
    `role` INT NOT NULL DEFAULT 2 COMMENT '角色：1-管理员，2-审核员',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. SKU表（sku）
CREATE TABLE IF NOT EXISTS `sku` (
    `sku_id` VARCHAR(36) NOT NULL COMMENT 'SKU唯一标识',
    `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称',
    `original_price` DECIMAL(10,2) NOT NULL COMMENT '原价',
    PRIMARY KEY (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU表';

-- 3. 活动表（promotion）
CREATE TABLE IF NOT EXISTS `promotion` (
    `promotion_id` VARCHAR(36) NOT NULL COMMENT '活动唯一标识',
    `name` VARCHAR(100) NOT NULL COMMENT '促销名称',
    `stime` DATETIME NOT NULL COMMENT '开始时间',
    `etime` DATETIME NOT NULL COMMENT '结束时间',
    `creator` VARCHAR(36) NOT NULL COMMENT '创建人',
    `operator` VARCHAR(36) DEFAULT NULL COMMENT '最近操作人',
    `status` INT NOT NULL DEFAULT 0 COMMENT '活动状态：0-草稿，1-审核中，2-待生效，3-生效中，4-过时，5-下线',
    `audit_status` INT NOT NULL DEFAULT 0 COMMENT '审核状态：0-等待审核，1-审核中，2-审核通过，3-审核驳回，4-审核不通过，5-审核拟作废',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`promotion_id`),
    KEY `idx_creator` (`creator`),
    KEY `idx_operator` (`operator`),
    KEY `idx_stime_etime` (`stime`, `etime`),
    CONSTRAINT `fk_promotion_creator` FOREIGN KEY (`creator`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_promotion_operator` FOREIGN KEY (`operator`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- 4. 活动-SKU关联表（promotion_sku）
CREATE TABLE IF NOT EXISTS `promotion_sku` (
    `id` VARCHAR(36) NOT NULL COMMENT '记录唯一标识',
    `promotion_id` VARCHAR(36) NOT NULL COMMENT '活动ID',
    `sku_id` VARCHAR(36) NOT NULL COMMENT 'SKU ID',
    `discount` DECIMAL(4,2) NOT NULL COMMENT '折扣（0.01-1.00）',
    PRIMARY KEY (`id`),
    KEY `idx_promotion_id` (`promotion_id`),
    KEY `idx_sku_id` (`sku_id`),
    CONSTRAINT `fk_promo_sku_promotion` FOREIGN KEY (`promotion_id`) REFERENCES `promotion` (`promotion_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_promo_sku_sku` FOREIGN KEY (`sku_id`) REFERENCES `sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动-SKU关联表';

-- 5. 审核记录表（audit_record）
CREATE TABLE IF NOT EXISTS `audit_record` (
    `audit_id` VARCHAR(36) NOT NULL COMMENT '审核记录唯一标识',
    `promotion_id` VARCHAR(36) NOT NULL COMMENT '关联活动ID',
    `audit_status` INT NOT NULL DEFAULT 0 COMMENT '审核状态：0-等待审核，1-审核中，2-审核通过，3-审核驳回，4-审核不通过，5-审核拟作废',
    `submit_time` DATETIME DEFAULT NULL COMMENT '提交审核时间',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成审核时间',
    `auditor_id` VARCHAR(36) DEFAULT NULL COMMENT '审核员用户ID',
    `comment` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
    `ctime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`audit_id`),
    KEY `idx_audit_promotion_id` (`promotion_id`),
    CONSTRAINT `fk_audit_record_promotion` FOREIGN KEY (`promotion_id`) REFERENCES `promotion` (`promotion_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核记录表';

-- 6. 事件日志表（event_log）
CREATE TABLE IF NOT EXISTS `event_log` (
    `event_id` VARCHAR(36) NOT NULL COMMENT '事件唯一标识',
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型',
    `promotion_id` VARCHAR(36) DEFAULT NULL COMMENT '活动ID',
    `prev_activity_status` INT DEFAULT NULL COMMENT '前置活动状态',
    `prev_audit_status` INT DEFAULT NULL COMMENT '前置审核状态',
    `operator` VARCHAR(36) DEFAULT NULL COMMENT '操作人',
    `event_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
    `params` TEXT COMMENT '事件参数JSON',
    PRIMARY KEY (`event_id`),
    KEY `idx_promotion_id` (`promotion_id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_event_time` (`event_time`),
    CONSTRAINT `fk_event_promotion` FOREIGN KEY (`promotion_id`) REFERENCES `promotion` (`promotion_id`),
    CONSTRAINT `fk_event_operator` FOREIGN KEY (`operator`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件日志表';

-- ============================================
-- 第三步：插入测试数据
-- ============================================
-- 状态码参考:
--   PromotionStatus: 0=草稿 1=审核中 2=待生效 3=生效中 4=过时 5=下线
--   AuditStatus:     0=等待审核 1=审核中 2=审核通过 3=审核驳回 4=审核不通过 5=审核拟作废
--   EventType: E_CREATE_DRAFT / E_SUBMIT_AUDIT / E_AUDIT_PASS / E_AUDIT_REJECT
--              E_AUDIT_NOTPASS / E_AUDIT_CANCEL / E_ACTIVE_ONLINE / E_ACTIVE_EXPIRE
--              E_MANUAL_OFFLINE / E_UPDATE_ACTIVITY / E_DELETE_ACTIVITY

-- 1. 用户 (10条)
INSERT INTO `user` (`user_id`, `username`, `password`, `role`, `ctime`) VALUES
('SYSTEM', 'system',   '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 1, DATE_SUB(NOW(), INTERVAL 120 DAY)),
('u001',  'admin',     '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 1, DATE_SUB(NOW(), INTERVAL 120 DAY)),
('u002',  'zhangsan',  '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 1, DATE_SUB(NOW(), INTERVAL 100 DAY)),
('u003',  'lisi',      '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 1, DATE_SUB(NOW(), INTERVAL 90 DAY)),
('u004',  'wangwu',    '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 1, DATE_SUB(NOW(), INTERVAL 80 DAY)),
('u005',  'zhaoliu',   '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 1, DATE_SUB(NOW(), INTERVAL 70 DAY)),
('u006',  'auditor01', '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 2, DATE_SUB(NOW(), INTERVAL 110 DAY)),
('u007',  'auditor02', '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 2, DATE_SUB(NOW(), INTERVAL 100 DAY)),
('u008',  'auditor03', '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 2, DATE_SUB(NOW(), INTERVAL 90 DAY)),
('u009',  'auditor04', '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 2, DATE_SUB(NOW(), INTERVAL 80 DAY));

-- 2. SKU (12条)
INSERT INTO `sku` (`sku_id`, `sku_name`, `original_price`) VALUES
('sku001', 'iPhone 15 Pro 128GB 黑色',          7999.00),
('sku002', 'MacBook Air M3 15英寸 256GB',        10499.00),
('sku003', 'AirPods Pro 第二代',                  1899.00),
('sku004', 'iPad Air 11英寸 128GB',              4799.00),
('sku005', 'Apple Watch Series 9 GPS 45mm',      3199.00),
('sku006', 'Sony WH-1000XM5 无线降噪耳机',        2299.00),
('sku007', 'Nintendo Switch OLED 马力欧红蓝',      2199.00),
('sku008', 'Dyson V15 Detect 无绳吸尘器',         4990.00),
('sku009', '戴尔 U2724D 27英寸 2K 显示器',         3299.00),
('sku010', '罗技 MX Master 3S 无线鼠标',           699.00),
('sku011', '三星 T7 Shield 2TB 移动固态硬盘',      1299.00),
('sku012', 'Anker 737 移动电源 24000mAh',          499.00);

-- 3. 活动 (14条) — 覆盖全部状态组合
INSERT INTO `promotion` (`promotion_id`, `name`, `stime`, `etime`, `creator`, `operator`, `status`, `audit_status`, `ctime`) VALUES
-- [草稿阶段]
('promo001', '年终清仓特卖',            '2026-12-20 00:00:00', '2026-12-31 23:59:59', 'u001', 'u001', 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('promo002', '开学季数码焕新',          '2026-09-01 00:00:00', '2026-09-15 23:59:59', 'u002', 'u002', 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
('promo003', '三八女神节美妆专场',      '2026-03-01 00:00:00', '2026-03-10 23:59:59', 'u003', 'u003', 0, 3, DATE_SUB(NOW(), INTERVAL 10 DAY)),
-- [待生效]
('promo004', '国庆黄金周大促',          '2026-10-01 00:00:00', '2026-10-07 23:59:59', 'u001', 'u006', 2, 2, DATE_SUB(NOW(), INTERVAL 12 DAY)),
('promo005', '双十一狂欢预售',          '2026-11-01 00:00:00', '2026-11-11 23:59:59', 'u004', 'u007', 2, 2, DATE_SUB(NOW(), INTERVAL 20 DAY)),
-- [生效中]
('promo006', '618数码家电节',           '2026-05-20 00:00:00', '2026-06-25 23:59:59', 'u001', 'u001', 3, 2, DATE_SUB(NOW(), INTERVAL 30 DAY)),
('promo007', '夏日清凉特惠',            '2026-06-01 00:00:00', '2026-08-31 23:59:59', 'u002', 'u002', 3, 2, DATE_SUB(NOW(), INTERVAL 20 DAY)),
('promo008', '会员专属折扣周',          '2026-06-05 00:00:00', '2026-06-12 23:59:59', 'u005', 'u008', 3, 2, DATE_SUB(NOW(), INTERVAL 10 DAY)),
-- [已过期]
('promo009', '春节年货大集',            '2026-01-15 00:00:00', '2026-02-05 23:59:59', 'u001', 'SYSTEM', 4, 2, DATE_SUB(NOW(), INTERVAL 80 DAY)),
('promo010', '五一出行装备节',          '2026-04-25 00:00:00', '2026-05-05 23:59:59', 'u003', 'SYSTEM', 4, 2, DATE_SUB(NOW(), INTERVAL 50 DAY)),
-- [已下线]
('promo011', '情人节限定礼盒',          '2026-02-10 00:00:00', '2026-02-16 23:59:59', 'u001', 'u001', 5, 2, DATE_SUB(NOW(), INTERVAL 70 DAY)),
('promo012', '品牌日特卖（违规下线）',  '2026-05-01 00:00:00', '2026-05-10 23:59:59', 'u002', 'u002', 5, 2, DATE_SUB(NOW(), INTERVAL 40 DAY)),
-- [审核作废/不通过]
('promo013', '测试活动（审核作废）',    '2026-07-01 00:00:00', '2026-07-07 23:59:59', 'u004', 'u009', 5, 5, DATE_SUB(NOW(), INTERVAL 15 DAY)),
('promo014', '过期活动（审核不通过）',  '2026-03-10 00:00:00', '2026-03-20 23:59:59', 'u005', 'u007', 5, 4, DATE_SUB(NOW(), INTERVAL 60 DAY));

-- 4. 活动-SKU关联 (18条)
INSERT INTO `promotion_sku` (`id`, `promotion_id`, `sku_id`, `discount`) VALUES
-- promo001: 草稿，3个SKU
('ps001', 'promo001', 'sku001', 0.88),
('ps002', 'promo001', 'sku002', 0.90),
('ps003', 'promo001', 'sku003', 0.85),
-- promo002: 审核中，2个SKU
('ps004', 'promo002', 'sku004', 0.92),
('ps005', 'promo002', 'sku005', 0.88),
-- promo003: 驳回，1个SKU
('ps006', 'promo003', 'sku006', 0.75),
-- promo004: 待生效，2个SKU
('ps007', 'promo004', 'sku007', 0.85),
('ps008', 'promo004', 'sku008', 0.90),
-- promo005: 待生效，2个SKU
('ps009', 'promo005', 'sku009', 0.82),
('ps010', 'promo005', 'sku010', 0.78),
-- promo006: 生效中，4个SKU
('ps011', 'promo006', 'sku001', 0.85),
('ps012', 'promo006', 'sku002', 0.90),
('ps013', 'promo006', 'sku011', 0.88),
('ps014', 'promo006', 'sku012', 0.80),
-- promo007: 生效中，2个SKU
('ps015', 'promo007', 'sku003', 0.85),
('ps016', 'promo007', 'sku005', 0.90),
-- promo008: 生效中，1个SKU
('ps017', 'promo008', 'sku006', 0.75),
-- promo009: 已过期，1个SKU
('ps018', 'promo009', 'sku008', 0.82);

-- 5. 审核记录 (14条)
INSERT INTO `audit_record` (`audit_id`, `promotion_id`, `audit_status`, `submit_time`, `complete_time`, `auditor_id`, `comment`, `ctime`) VALUES
('ar001',  'promo001', 0, NULL,                                       NULL,                                       NULL,    NULL,                    DATE_SUB(NOW(), INTERVAL 2 DAY)),
('ar002',  'promo002', 1, DATE_SUB(NOW(), INTERVAL 3 DAY),            NULL,                                       NULL,    NULL,                    DATE_SUB(NOW(), INTERVAL 5 DAY)),
('ar003',  'promo003', 3, DATE_SUB(NOW(), INTERVAL 7 DAY),            DATE_SUB(NOW(), INTERVAL 6 DAY),           'u006',  '商品毛利不足，请调整折扣', DATE_SUB(NOW(), INTERVAL 10 DAY)),
('ar004',  'promo004', 2, DATE_SUB(NOW(), INTERVAL 9 DAY),            DATE_SUB(NOW(), INTERVAL 8 DAY),           'u006',  '审核通过，活动方案完整',    DATE_SUB(NOW(), INTERVAL 12 DAY)),
('ar005',  'promo005', 2, DATE_SUB(NOW(), INTERVAL 17 DAY),           DATE_SUB(NOW(), INTERVAL 16 DAY),          'u007',  '同意',                    DATE_SUB(NOW(), INTERVAL 20 DAY)),
('ar006',  'promo006', 2, DATE_SUB(NOW(), INTERVAL 28 DAY),           DATE_SUB(NOW(), INTERVAL 27 DAY),          'u006',  '大型活动已备案，通过',      DATE_SUB(NOW(), INTERVAL 30 DAY)),
('ar007',  'promo007', 2, DATE_SUB(NOW(), INTERVAL 17 DAY),           DATE_SUB(NOW(), INTERVAL 16 DAY),          'u008',  '审核通过',                DATE_SUB(NOW(), INTERVAL 20 DAY)),
('ar008',  'promo008', 2, DATE_SUB(NOW(), INTERVAL 7 DAY),            DATE_SUB(NOW(), INTERVAL 6 DAY),           'u008',  '同意上线',                DATE_SUB(NOW(), INTERVAL 10 DAY)),
('ar009',  'promo009', 2, DATE_SUB(NOW(), INTERVAL 78 DAY),           DATE_SUB(NOW(), INTERVAL 77 DAY),          'u007',  '通过',                    DATE_SUB(NOW(), INTERVAL 80 DAY)),
('ar010',  'promo010', 2, DATE_SUB(NOW(), INTERVAL 48 DAY),           DATE_SUB(NOW(), INTERVAL 47 DAY),          'u006',  '审核通过',                DATE_SUB(NOW(), INTERVAL 50 DAY)),
('ar011',  'promo011', 2, DATE_SUB(NOW(), INTERVAL 68 DAY),           DATE_SUB(NOW(), INTERVAL 67 DAY),          'u007',  '通过',                    DATE_SUB(NOW(), INTERVAL 70 DAY)),
('ar012',  'promo012', 2, DATE_SUB(NOW(), INTERVAL 38 DAY),           DATE_SUB(NOW(), INTERVAL 37 DAY),          'u008',  '审核通过',                DATE_SUB(NOW(), INTERVAL 40 DAY)),
('ar013',  'promo013', 5, DATE_SUB(NOW(), INTERVAL 13 DAY),           DATE_SUB(NOW(), INTERVAL 12 DAY),          'u009',  '活动方案不合规，作废处理',  DATE_SUB(NOW(), INTERVAL 15 DAY)),
('ar014',  'promo014', 4, DATE_SUB(NOW(), INTERVAL 58 DAY),           DATE_SUB(NOW(), INTERVAL 57 DAY),          'u007',  '商品资质不全，不予通过',    DATE_SUB(NOW(), INTERVAL 60 DAY));

-- 6. 事件日志 (20条) — 覆盖全事件类型
INSERT INTO `event_log` (`event_id`, `event_type`, `promotion_id`, `prev_activity_status`, `prev_audit_status`, `operator`, `event_time`, `params`) VALUES
-- promo001: 草稿 [1条]
('evt001', 'E_CREATE_DRAFT',  'promo001', NULL, NULL, 'u001', DATE_SUB(NOW(), INTERVAL 2 DAY),  '{"name":"年终清仓特卖"}'),

-- promo002: 草稿→审核中 [2条]
('evt002', 'E_CREATE_DRAFT',  'promo002', NULL, NULL, 'u002', DATE_SUB(NOW(), INTERVAL 5 DAY),  '{"name":"开学季数码焕新"}'),
('evt003', 'E_SUBMIT_AUDIT',  'promo002', 0,    0,    'u002', DATE_SUB(NOW(), INTERVAL 3 DAY),  '{}'),

-- promo003: 草稿→审核中→驳回 [3条]
('evt004', 'E_CREATE_DRAFT',  'promo003', NULL, NULL, 'u003', DATE_SUB(NOW(), INTERVAL 10 DAY), '{"name":"三八女神节美妆专场"}'),
('evt005', 'E_SUBMIT_AUDIT',  'promo003', 0,    0,    'u003', DATE_SUB(NOW(), INTERVAL 7 DAY),  '{}'),
('evt006', 'E_AUDIT_REJECT',  'promo003', 1,    1,    'u006', DATE_SUB(NOW(), INTERVAL 6 DAY),  '{"comment":"商品毛利不足，请调整折扣"}'),

-- promo004: 完整正向链路 草稿→审核中→通过→待生效 [3条]
('evt007', 'E_CREATE_DRAFT',  'promo004', NULL, NULL, 'u001', DATE_SUB(NOW(), INTERVAL 12 DAY), '{"name":"国庆黄金周大促"}'),
('evt008', 'E_SUBMIT_AUDIT',  'promo004', 0,    0,    'u001', DATE_SUB(NOW(), INTERVAL 9 DAY),  '{}'),
('evt009', 'E_AUDIT_PASS',    'promo004', 1,    1,    'u006', DATE_SUB(NOW(), INTERVAL 8 DAY),  '{"comment":"审核通过，活动方案完整"}'),

-- promo005: 草稿→审核中→通过→待生效 [3条] (跨审核员)
('evt010', 'E_CREATE_DRAFT',  'promo005', NULL, NULL, 'u004', DATE_SUB(NOW(), INTERVAL 20 DAY), '{"name":"双十一狂欢预售"}'),
('evt011', 'E_SUBMIT_AUDIT',  'promo005', 0,    0,    'u004', DATE_SUB(NOW(), INTERVAL 17 DAY), '{}'),
('evt012', 'E_AUDIT_PASS',    'promo005', 1,    1,    'u007', DATE_SUB(NOW(), INTERVAL 16 DAY), '{"comment":"同意"}'),

-- promo006: 最完整链路 草稿→审核中→通过→待生效→上线 [4条]
('evt013', 'E_CREATE_DRAFT',  'promo006', NULL, NULL, 'u001', DATE_SUB(NOW(), INTERVAL 30 DAY), '{"name":"618数码家电节"}'),
('evt014', 'E_SUBMIT_AUDIT',  'promo006', 0,    0,    'u001', DATE_SUB(NOW(), INTERVAL 28 DAY), '{}'),
('evt015', 'E_AUDIT_PASS',    'promo006', 1,    1,    'u006', DATE_SUB(NOW(), INTERVAL 27 DAY), '{"comment":"大型活动已备案，通过"}'),
('evt016', 'E_ACTIVE_ONLINE', 'promo006', 2,    2,    'SYSTEM', DATE_SUB(NOW(), INTERVAL 20 DAY), '{}'),

-- promo009: 完整链路含自动过期 [1条额外] (已有CREATE/SUBMIT/PASS/ONLINE)
('evt017', 'E_ACTIVE_EXPIRE', 'promo009', 3,    2,    'SYSTEM', DATE_SUB(NOW(), INTERVAL 5 DAY),  '{}'),

-- promo011: 手动下线 [1条] (已有CREATE/SUBMIT/PASS/ONLINE)
('evt018', 'E_MANUAL_OFFLINE','promo011', 3,    2,    'u001', DATE_SUB(NOW(), INTERVAL 65 DAY), '{"reason":"违反平台规则，紧急下线"}'),

-- promo013: 审核作废 [1条]
('evt019', 'E_AUDIT_CANCEL',  'promo013', 0,    0,    'u009', DATE_SUB(NOW(), INTERVAL 12 DAY), '{"comment":"活动方案不合规，作废处理"}'),

-- promo014: 审核不通过 [1条]
('evt020', 'E_AUDIT_NOTPASS', 'promo014', 1,    1,    'u007', DATE_SUB(NOW(), INTERVAL 57 DAY), '{"comment":"商品资质不全，不予通过"}');

