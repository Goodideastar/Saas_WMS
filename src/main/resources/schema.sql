-- WMS Database Initialization Script

CREATE DATABASE IF NOT EXISTS wms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE wms;

-- System tables
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1 COMMENT '0=disabled, 1=enabled',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(20) COMMENT 'menu,button,api',
    parent_id BIGINT DEFAULT 0,
    path VARCHAR(255),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- WMS business tables
CREATE TABLE wms_warehouse (
    id BIGINT PRIMARY KEY,
    warehouse_code VARCHAR(50) NOT NULL UNIQUE,
    warehouse_name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    contact_person VARCHAR(50),
    contact_phone VARCHAR(20),
    status TINYINT DEFAULT 1,
    remark TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_product (
    id BIGINT PRIMARY KEY,
    product_code VARCHAR(50) NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    specification VARCHAR(100),
    unit VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    image_url VARCHAR(500),
    reference_cost DECIMAL(10,2),
    reference_price DECIMAL(10,2),
    current_stock INT DEFAULT 0,
    alert_min INT DEFAULT 0,
    alert_max INT DEFAULT 99999,
    status TINYINT DEFAULT 1,
    remark TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_inbound_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    warehouse_id BIGINT,
    supplier VARCHAR(100),
    inbound_type VARCHAR(50) COMMENT 'PURCHASE,RETURN,INVENTORY',
    related_order_no VARCHAR(50),
    operator_id BIGINT,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING,COMPLETED,CANCELLED',
    inbound_time DATETIME,
    remark TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_inbound_order_item (
    id BIGINT PRIMARY KEY,
    inbound_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    expected_quantity INT NOT NULL,
    actual_quantity INT NOT NULL,
    unit_price DECIMAL(10,2),
    subtotal DECIMAL(12,2),
    batch_no VARCHAR(50),
    production_date DATE,
    expiry_date DATE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_outbound_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    warehouse_id BIGINT,
    customer VARCHAR(100),
    outbound_type VARCHAR(50) COMMENT 'SALES,MATERIAL_LOSS,INVENTORY',
    related_order_no VARCHAR(50),
    operator_id BIGINT,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING,COMPLETED,CANCELLED',
    outbound_time DATETIME,
    remark TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_outbound_order_item (
    id BIGINT PRIMARY KEY,
    outbound_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    expected_quantity INT NOT NULL,
    actual_quantity INT NOT NULL,
    unit_price DECIMAL(10,2),
    subtotal DECIMAL(12,2),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_stock_log (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT,
    operation_type VARCHAR(50) COMMENT 'IN,OUT,ADJUST',
    quantity_before INT,
    quantity_change INT,
    quantity_after INT,
    related_order_no VARCHAR(50),
    remark TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_stock_alert (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT,
    alert_type VARCHAR(20) COMMENT 'BELOW_MIN,ABOVE_MAX',
    alert_value INT,
    actual_stock INT,
    status VARCHAR(20) DEFAULT 'UNHANDLED' COMMENT 'UNHANDLED,HANDLED',
    handle_remark TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE wms_operation_log (
    id BIGINT PRIMARY KEY,
    operator VARCHAR(50),
    ip VARCHAR(50),
    request_url VARCHAR(255),
    method_name VARCHAR(200),
    params TEXT,
    duration BIGINT,
    status TINYINT COMMENT '0=fail, 1=success',
    result TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    version INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default admin user (password: admin123)
INSERT INTO sys_user (id, username, password, email, phone, status, create_by, update_by) 
VALUES (1, 'admin', '$2a$10$rCZjF8bJQ8fMqK3qL0jYhOL9g8F0h7K3lM9nO2pQ5rS7tU8vW9xYz', 'admin@wms.com', '13800138000', 1, 'system', 'system');

-- Insert default roles
INSERT INTO sys_role (id, role_code, role_name, description, status, create_by, update_by) 
VALUES (1, 'ADMIN', '系统管理员', '系统管理员拥有所有权限', 1, 'system', 'system');
INSERT INTO sys_role (id, role_code, role_name, description, status, create_by, update_by) 
VALUES (2, 'WAREHOUSE_MANAGER', '仓库管理员', '仓库管理员负责出入库管理', 1, 'system', 'system');
INSERT INTO sys_role (id, role_code, role_name, description, status, create_by, update_by) 
VALUES (3, 'OPERATOR', '操作员', '操作员负责日常操作', 1, 'system', 'system');

-- Insert default permissions
INSERT INTO sys_permission (id, permission_code, permission_name, resource_type, parent_id, path, status, create_by, update_by) VALUES
(1, 'product:list', '货品列表', 'button', 0, '', 1, 'system', 'system'),
(2, 'product:add', '新增货品', 'button', 0, '', 1, 'system', 'system'),
(3, 'product:edit', '修改货品', 'button', 0, '', 1, 'system', 'system'),
(4, 'product:delete', '删除货品', 'button', 0, '', 1, 'system', 'system'),
(5, 'product:adjust', '调整库存', 'button', 0, '', 1, 'system', 'system'),
(6, 'inbound:create', '创建入库单', 'button', 0, '', 1, 'system', 'system'),
(7, 'inbound:audit', '审核入库单', 'button', 0, '', 1, 'system', 'system'),
(8, 'inbound:cancel', '取消入库单', 'button', 0, '', 1, 'system', 'system'),
(9, 'inbound:query', '查询入库单', 'button', 0, '', 1, 'system', 'system'),
(10, 'outbound:create', '创建出库单', 'button', 0, '', 1, 'system', 'system'),
(11, 'outbound:audit', '审核出库单', 'button', 0, '', 1, 'system', 'system'),
(12, 'outbound:cancel', '取消出库单', 'button', 0, '', 1, 'system', 'system'),
(13, 'outbound:query', '查询出库单', 'button', 0, '', 1, 'system', 'system'),
(14, 'alert:query', '查询预警', 'button', 0, '', 1, 'system', 'system'),
(15, 'alert:handle', '处理预警', 'button', 0, '', 1, 'system', 'system'),
(16, 'dashboard:query', '查看看板', 'button', 0, '', 1, 'system', 'system');

-- Assign admin role to admin user
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- Assign all permissions to admin role
INSERT INTO sys_role_permission (role_id, permission_id) 
SELECT 1, id FROM sys_permission;

-- Insert default warehouse
INSERT INTO wms_warehouse (id, warehouse_code, warehouse_name, address, contact_person, contact_phone, status, create_by, update_by) 
VALUES (1, 'WH001', '主仓库', '北京市朝阳区', '张三', '13800138001', 1, 'system', 'system');
