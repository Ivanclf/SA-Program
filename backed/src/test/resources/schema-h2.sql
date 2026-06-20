-- ============================================
-- H2 集成测试 Schema + 种子数据
-- MySQL 兼容模式: MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER
-- ============================================

DROP TABLE IF EXISTS event_log;
DROP TABLE IF EXISTS promotion_sku;
DROP TABLE IF EXISTS audit_record;
DROP TABLE IF EXISTS promotion;
DROP TABLE IF EXISTS sku;
DROP TABLE IF EXISTS "user";

-- ============================================
-- 建表
-- ============================================

-- 1. 用户表
CREATE TABLE "user" (
    user_id VARCHAR(36) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role INT NOT NULL DEFAULT 2,
    ctime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    utime TIMESTAMP NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_username UNIQUE (username)
);

-- 2. SKU表
CREATE TABLE sku (
    sku_id VARCHAR(36) NOT NULL,
    sku_name VARCHAR(200) NOT NULL,
    original_price DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (sku_id)
);

-- 3. 活动表
CREATE TABLE promotion (
    promotion_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    stime TIMESTAMP NOT NULL,
    etime TIMESTAMP NOT NULL,
    creator VARCHAR(36) NOT NULL,
    operator VARCHAR(36) NULL,
    status INT NOT NULL DEFAULT 0,
    audit_status INT NOT NULL DEFAULT 0,
    ctime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    utime TIMESTAMP NULL,
    PRIMARY KEY (promotion_id),
    CONSTRAINT fk_promotion_creator FOREIGN KEY (creator) REFERENCES "user" (user_id),
    CONSTRAINT fk_promotion_operator FOREIGN KEY (operator) REFERENCES "user" (user_id)
);
CREATE INDEX idx_promotion_creator ON promotion(creator);
CREATE INDEX idx_promotion_operator ON promotion(operator);

-- 4. 活动-SKU关联表
CREATE TABLE promotion_sku (
    id VARCHAR(36) NOT NULL,
    promotion_id VARCHAR(36) NOT NULL,
    sku_id VARCHAR(36) NOT NULL,
    discount DECIMAL(4,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_promo_sku_promotion FOREIGN KEY (promotion_id) REFERENCES promotion (promotion_id) ON DELETE CASCADE,
    CONSTRAINT fk_promo_sku_sku FOREIGN KEY (sku_id) REFERENCES sku (sku_id)
);
CREATE INDEX idx_ps_promotion_id ON promotion_sku(promotion_id);
CREATE INDEX idx_ps_sku_id ON promotion_sku(sku_id);

-- 5. 审核记录表
CREATE TABLE audit_record (
    audit_id VARCHAR(36) NOT NULL,
    promotion_id VARCHAR(36) NOT NULL,
    audit_status INT NOT NULL DEFAULT 0,
    submit_time TIMESTAMP NULL,
    complete_time TIMESTAMP NULL,
    auditor_id VARCHAR(36) NULL,
    comment VARCHAR(500) NULL,
    ctime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    utime TIMESTAMP NULL,
    PRIMARY KEY (audit_id),
    CONSTRAINT fk_audit_record_promotion FOREIGN KEY (promotion_id) REFERENCES promotion (promotion_id) ON DELETE CASCADE
);
CREATE INDEX idx_audit_promotion_id ON audit_record(promotion_id);

-- 6. 事件日志表
CREATE TABLE event_log (
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    promotion_id VARCHAR(36) NULL,
    prev_activity_status INT NULL,
    prev_audit_status INT NULL,
    operator VARCHAR(36) NULL,
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    params TEXT NULL,
    PRIMARY KEY (event_id),
    CONSTRAINT fk_event_promotion FOREIGN KEY (promotion_id) REFERENCES promotion (promotion_id) ON DELETE CASCADE,
    CONSTRAINT fk_event_operator FOREIGN KEY (operator) REFERENCES "user" (user_id)
);
CREATE INDEX idx_event_promotion_id ON event_log(promotion_id);
CREATE INDEX idx_event_type ON event_log(event_type);

-- ============================================
-- 种子数据
-- ============================================
-- PromotionStatus: 0=草稿 1=审核中 2=待生效 3=生效中 4=过时 5=下线
-- AuditStatus:     0=等待审核 1=审核中 2=审核通过 3=审核驳回 4=审核不通过 5=审核作废

-- 用户（密码 BCrypt 哈希仅用于 FK 约束，登录测试会先注册新用户）
INSERT INTO "user" (user_id, username, password, role, ctime) VALUES
('u_sys',      'system',       '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 1, '2025-01-01 00:00:00'),
('u_seed',     'seeduser',     '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 1, '2025-06-01 00:00:00'),
('u_auditor',  'auditor01',    '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 2, '2025-06-01 00:00:00'),
('u_existing', 'existing_user', '$2a$10$mwvbuL58c9xn8xsH6GydVepB0pOuAcF95PtSZ1a7KmRb8ocoILSAK', 1, '2025-06-01 00:00:00');

-- SKUs
INSERT INTO sku (sku_id, sku_name, original_price) VALUES
('sku001', 'iPhone 15 Pro 128GB', 7999.00),
('sku002', 'MacBook Air M3 15inch', 10499.00),
('sku003', 'AirPods Pro 2nd Gen', 1899.00);

-- Promotion seed data (covering all statuses)
INSERT INTO promotion (promotion_id, name, stime, etime, creator, operator, status, audit_status, ctime) VALUES
('promo_draft',    'Draft Promo',    '2026-12-01 00:00:00', '2026-12-31 23:59:59', 'u_seed', 'u_seed',    0, 0, '2026-06-15 00:00:00'),
('promo_auditing', 'Auditing Promo', '2026-11-01 00:00:00', '2026-11-30 23:59:59', 'u_seed', 'u_seed',    1, 1, '2026-06-10 00:00:00'),
('promo_init',     'Pending Promo',  '2026-10-01 00:00:00', '2026-10-31 23:59:59', 'u_seed', 'u_auditor', 2, 2, '2026-06-01 00:00:00'),
('promo_online',   'Active Promo',   '2026-06-01 00:00:00', '2026-08-31 23:59:59', 'u_seed', 'u_seed',    3, 2, '2026-05-01 00:00:00'),
('promo_expired',  'Expired Promo',  '2025-01-01 00:00:00', '2025-02-01 23:59:59', 'u_seed', 'u_sys',     4, 2, '2024-12-01 00:00:00'),
('promo_offline',  'Offline Promo',  '2026-03-01 00:00:00', '2026-04-30 23:59:59', 'u_seed', 'u_seed',    5, 2, '2026-02-01 00:00:00');

-- 活动-SKU关联
INSERT INTO promotion_sku (id, promotion_id, sku_id, discount) VALUES
('ps001', 'promo_draft',  'sku001', 0.90),
('ps002', 'promo_online', 'sku001', 0.85),
('ps003', 'promo_online', 'sku002', 0.88);

-- 审核记录
INSERT INTO audit_record (audit_id, promotion_id, audit_status, submit_time, complete_time, auditor_id, comment, ctime) VALUES
('ar_draft',    'promo_draft',    0, NULL,                  NULL,                  NULL,        NULL,       '2026-06-15 00:00:00'),
('ar_auditing', 'promo_auditing', 1, '2026-06-12 00:00:00', NULL,                  NULL,        NULL,       '2026-06-10 00:00:00'),
('ar_init',     'promo_init',     2, '2026-06-05 00:00:00', '2026-06-06 00:00:00', 'u_auditor', 'Approved', '2026-06-01 00:00:00'),
('ar_online',   'promo_online',   2, '2026-05-05 00:00:00', '2026-05-06 00:00:00', 'u_auditor', 'Approved', '2026-05-01 00:00:00'),
('ar_expired',  'promo_expired',  2, '2024-12-05 00:00:00', '2024-12-06 00:00:00', 'u_auditor', 'Approved', '2024-12-01 00:00:00'),
('ar_offline',  'promo_offline',  2, '2026-02-05 00:00:00', '2026-02-06 00:00:00', 'u_auditor', 'Approved', '2026-02-01 00:00:00');

-- 事件日志
INSERT INTO event_log (event_id, event_type, promotion_id, prev_activity_status, prev_audit_status, operator, event_time, params) VALUES
('evt_draft',   'E_CREATE_DRAFT',  'promo_draft',    NULL, NULL, 'u_seed',    '2026-06-15 00:00:00', '{"name":"Draft Promo"}'),
('evt_audit1',  'E_CREATE_DRAFT',  'promo_auditing', NULL, NULL, 'u_seed',    '2026-06-10 00:00:00', '{"name":"Auditing Promo"}'),
('evt_audit2',  'E_SUBMIT_AUDIT',  'promo_auditing', 0,    0,    'u_seed',    '2026-06-12 00:00:00', '{}'),
('evt_online1', 'E_CREATE_DRAFT',  'promo_online',   NULL, NULL, 'u_seed',    '2026-05-01 00:00:00', '{"name":"Active Promo"}'),
('evt_online2', 'E_SUBMIT_AUDIT',  'promo_online',   0,    0,    'u_seed',    '2026-05-05 00:00:00', '{}'),
('evt_online3', 'E_AUDIT_PASS',    'promo_online',   1,    1,    'u_auditor', '2026-05-06 00:00:00', '{"comment":"Approved"}'),
('evt_online4', 'E_ACTIVE_ONLINE', 'promo_online',   2,    2,    'u_sys',     '2026-06-01 00:00:00', '{}');
