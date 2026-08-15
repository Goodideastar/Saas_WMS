-- WMS Database Initialization Script
-- 在 MySQL 客户端执行: mysql -h <host> -u root -p < schema.sql
-- 幂等：所有 CREATE TABLE 使用 IF NOT EXISTS，INSERT 使用 INSERT IGNORE（不覆盖已有数据）

CREATE DATABASE IF NOT EXISTS wms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE wms;

-- ============================================================
-- 系统表
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(100),
    phone       VARCHAR(20),
    status      TINYINT      DEFAULT 1 COMMENT '0=disabled,1=enabled',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   VARCHAR(50),
    update_by   VARCHAR(50),
    is_deleted  TINYINT      DEFAULT 0,
    version     INT          DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code   VARCHAR(50)  NOT NULL UNIQUE,
    role_name   VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status      TINYINT      DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   VARCHAR(50),
    update_by   VARCHAR(50),
    is_deleted  TINYINT      DEFAULT 0,
    version     INT          DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(20)  COMMENT 'menu,button,api',
    parent_id       BIGINT       DEFAULT 0,
    path            VARCHAR(255),
    status          TINYINT      DEFAULT 1,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       VARCHAR(50),
    update_by       VARCHAR(50),
    is_deleted      TINYINT      DEFAULT 0,
    version         INT          DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 基础业务表
-- ============================================================

CREATE TABLE IF NOT EXISTS wms_warehouse (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    warehouse_code   VARCHAR(50)  NOT NULL UNIQUE,
    warehouse_name   VARCHAR(100) NOT NULL,
    address          VARCHAR(255),
    contact_person   VARCHAR(50),
    contact_phone    VARCHAR(20),
    status           TINYINT      DEFAULT 1,
    remark           TEXT,
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by        VARCHAR(50),
    update_by        VARCHAR(50),
    is_deleted       TINYINT      DEFAULT 0,
    version          INT          DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wms_product (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_code    VARCHAR(50)   NOT NULL UNIQUE,
    product_name    VARCHAR(100)  NOT NULL,
    specification   VARCHAR(100),
    unit            VARCHAR(20)   NOT NULL,
    category        VARCHAR(50),
    image_url       VARCHAR(500),
    reference_cost  DECIMAL(10,2) DEFAULT 0,
    reference_price DECIMAL(10,2) DEFAULT 0,
    current_stock   INT           DEFAULT 0,
    alert_min       INT           DEFAULT 0,
    alert_max       INT           DEFAULT 99999,
    status          TINYINT       DEFAULT 1,
    remark          TEXT,
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by       VARCHAR(50),
    update_by       VARCHAR(50),
    is_deleted      TINYINT       DEFAULT 0,
    version         INT           DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 入库单
-- ============================================================

CREATE TABLE IF NOT EXISTS inbound_order (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no         VARCHAR(50)   NOT NULL UNIQUE,
    warehouse_id     BIGINT,
    inbound_type     VARCHAR(50)   COMMENT 'PURCHASE,RETURN,INVENTORY',
    supplier         VARCHAR(100),
    related_order_no VARCHAR(50),
    status           VARCHAR(20)   DEFAULT 'PENDING' COMMENT 'PENDING,AUDITING,COMPLETED,CANCELLED',
    total_amount     DECIMAL(12,2) DEFAULT 0,
    inbound_time     DATETIME,
    remark           TEXT,
    create_time      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by        VARCHAR(50),
    update_by        VARCHAR(50),
    is_deleted       TINYINT       DEFAULT 0,
    version          INT           DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS inbound_order_item (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id          BIGINT        NOT NULL,
    product_id        BIGINT        NOT NULL,
    product_code      VARCHAR(50),
    product_name      VARCHAR(100),
    expected_quantity INT           NOT NULL DEFAULT 0,
    actual_quantity   INT           NOT NULL DEFAULT 0,
    unit_price        DECIMAL(10,2) DEFAULT 0,
    subtotal          DECIMAL(12,2) DEFAULT 0,
    batch_no          VARCHAR(50),
    production_date   DATE,
    expiry_date       DATE,
    create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by         VARCHAR(50),
    update_by         VARCHAR(50),
    is_deleted        TINYINT       DEFAULT 0,
    version           INT           DEFAULT 0,
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 出库单
-- ============================================================

CREATE TABLE IF NOT EXISTS outbound_order (
    id               BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no         VARCHAR(50)   NOT NULL UNIQUE,
    warehouse_id     BIGINT,
    outbound_type    VARCHAR(50)   COMMENT 'SALES,MATERIAL_LOSS,INVENTORY',
    customer         VARCHAR(100),
    related_order_no VARCHAR(50),
    status           VARCHAR(20)   DEFAULT 'PENDING' COMMENT 'PENDING,AUDITING,COMPLETED,CANCELLED',
    total_amount     DECIMAL(12,2) DEFAULT 0,
    outbound_time    DATETIME,
    remark           TEXT,
    create_time      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by        VARCHAR(50),
    update_by        VARCHAR(50),
    is_deleted       TINYINT       DEFAULT 0,
    version          INT           DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS outbound_order_item (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id          BIGINT        NOT NULL,
    product_id        BIGINT        NOT NULL,
    product_code      VARCHAR(50),
    product_name      VARCHAR(100),
    expected_quantity INT           NOT NULL DEFAULT 0,
    actual_quantity   INT           NOT NULL DEFAULT 0,
    unit_price        DECIMAL(10,2) DEFAULT 0,
    subtotal          DECIMAL(12,2) DEFAULT 0,
    create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by         VARCHAR(50),
    update_by         VARCHAR(50),
    is_deleted        TINYINT       DEFAULT 0,
    version           INT           DEFAULT 0,
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 库存 & 预警
-- ============================================================

CREATE TABLE IF NOT EXISTS stock_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT       NOT NULL,
    product_code  VARCHAR(50),
    product_name  VARCHAR(100),
    stock_type    VARCHAR(20)  COMMENT 'IN,OUT,ADJUST',
    quantity      INT          NOT NULL,
    before_stock  INT          DEFAULT 0,
    after_stock   INT          DEFAULT 0,
    order_id      BIGINT,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_product_id (product_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS stock_alert (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT       NOT NULL,
    product_code  VARCHAR(50),
    product_name  VARCHAR(100),
    alert_type    VARCHAR(20)  COMMENT 'BELOW_MIN,ABOVE_MAX',
    alert_content TEXT,
    status        VARCHAR(20)  DEFAULT 'UNHANDLED' COMMENT 'UNHANDLED,HANDLED',
    alert_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    handle_time   DATETIME,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by     VARCHAR(50),
    update_by     VARCHAR(50),
    is_deleted    TINYINT      DEFAULT 0,
    version       INT          DEFAULT 0,
    KEY idx_product_id (product_id),
    KEY idx_status (status),
    KEY idx_alert_time (alert_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 操作日志（AOP 切面写入）
-- ============================================================

CREATE TABLE IF NOT EXISTS operation_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    operator      VARCHAR(50),
    ip            VARCHAR(50),
    request_url   VARCHAR(255),
    method_name   VARCHAR(200),
    params        TEXT,
    duration      BIGINT,
    status        TINYINT      DEFAULT 1 COMMENT '0=fail,1=success',
    result        TEXT,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by     VARCHAR(50),
    update_by     VARCHAR(50),
    is_deleted    TINYINT      DEFAULT 0,
    version       INT          DEFAULT 0,
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 初始化默认数据
-- ============================================================

-- 管理员密码: admin123  (BCrypt)
-- 仅在用户不存在时插入，避免每次初始化覆盖已有的密码
INSERT IGNORE INTO sys_user (id, username, password, email, phone, status, create_by, update_by)
VALUES (1, 'admin', '$2a$10$.Q.3vq3ND0R7v6ODGRAmYe6XnrVthVULGsnv1glErLOngdLFZUzHe',
        'admin@wms.com', '13800138000', 1, 'system', 'system');

INSERT IGNORE INTO sys_role (id, role_code, role_name, description, status, create_by, update_by)
VALUES (1, 'ADMIN',            '系统管理员', '拥有全部权限', 1, 'system', 'system'),
       (2, 'WAREHOUSE_MANAGER','仓库管理员', '出入库管理', 1, 'system', 'system'),
       (3, 'OPERATOR',         '操作员',     '日常操作',   1, 'system', 'system');

INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, resource_type, parent_id, path, status, create_by, update_by) VALUES
(1,'product:list','货品列表','button',0,'',1,'system','system'),
(2,'product:add','新增货品','button',0,'',1,'system','system'),
(3,'product:edit','修改货品','button',0,'',1,'system','system'),
(4,'product:delete','删除货品','button',0,'',1,'system','system'),
(5,'product:adjust','调整库存','button',0,'',1,'system','system'),
(6,'inbound:create','创建入库单','button',0,'',1,'system','system'),
(7,'inbound:audit','审核入库单','button',0,'',1,'system','system'),
(8,'inbound:cancel','取消入库单','button',0,'',1,'system','system'),
(9,'inbound:query','查询入库单','button',0,'',1,'system','system'),
(10,'outbound:create','创建出库单','button',0,'',1,'system','system'),
(11,'outbound:audit','审核出库单','button',0,'',1,'system','system'),
(12,'outbound:cancel','取消出库单','button',0,'',1,'system','system'),
(13,'outbound:query','查询出库单','button',0,'',1,'system','system'),
(14,'alert:query','查询预警','button',0,'',1,'system','system'),
(15,'alert:handle','处理预警','button',0,'',1,'system','system'),
(16,'dashboard:query','查看看板','button',0,'',1,'system','system');

REPLACE INTO sys_user_role (id, user_id, role_id) VALUES (1, 1, 1);

INSERT IGNORE INTO sys_role_permission (id, role_id, permission_id)
SELECT id, 1, id FROM sys_permission;

INSERT IGNORE INTO wms_warehouse (id, warehouse_code, warehouse_name, address, contact_person, contact_phone, status, create_by, update_by)
VALUES (1, 'WH001', '主仓库', '北京市朝阳区', '张三', '13800138001', 1, 'system', 'system');
