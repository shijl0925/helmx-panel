-- PostgreSQL 兼容的表结构
CREATE TABLE IF NOT EXISTS tb_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100) NOT NULL,
    status INTEGER DEFAULT 1,
    is_super_admin BOOLEAN DEFAULT FALSE,
    depart_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS '
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
' LANGUAGE 'plpgsql';

-- 为 tb_users 表创建更新时间触发器
DROP TRIGGER IF EXISTS update_tb_users_updated_at ON tb_users;
CREATE TRIGGER update_tb_users_updated_at 
    BEFORE UPDATE ON tb_users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE IF NOT EXISTS tb_rbac_roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    remark VARCHAR(255),
    status INTEGER DEFAULT 1,
    code VARCHAR(50) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为 tb_rbac_roles 表创建更新时间触发器
DROP TRIGGER IF EXISTS update_tb_rbac_roles_updated_at ON tb_rbac_roles;
CREATE TRIGGER update_tb_rbac_roles_updated_at 
    BEFORE UPDATE ON tb_rbac_roles 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE IF NOT EXISTS tb_rbac_user_roles (
    user_id BIGINT,
    role_id BIGINT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES tb_users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES tb_rbac_roles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tb_rbac_menus (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    parent_id BIGINT,
    type VARCHAR(32) NOT NULL,
    auth_code VARCHAR(64),
    path VARCHAR(64),
    component VARCHAR(64),
    status INTEGER DEFAULT 1,
    active_path VARCHAR(64),
    icon VARCHAR(64),
    "order" INTEGER,
    title VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为 tb_rbac_menus 表创建更新时间触发器
DROP TRIGGER IF EXISTS update_tb_rbac_menus_updated_at ON tb_rbac_menus;
CREATE TRIGGER update_tb_rbac_menus_updated_at 
    BEFORE UPDATE ON tb_rbac_menus 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE IF NOT EXISTS tb_rbac_role_menus (
    role_id BIGINT,
    menu_id BIGINT,
    PRIMARY KEY (role_id, menu_id),
    FOREIGN KEY (role_id) REFERENCES tb_rbac_roles(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_id) REFERENCES tb_rbac_menus(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tb_rbac_resources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    parent_id BIGINT,
    status INTEGER DEFAULT 1,
    type VARCHAR(32) NOT NULL,
    path VARCHAR(64) NOT NULL,
    method VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为 tb_rbac_resources 表创建更新时间触发器
DROP TRIGGER IF EXISTS update_tb_rbac_resources_updated_at ON tb_rbac_resources;
CREATE TRIGGER update_tb_rbac_resources_updated_at 
    BEFORE UPDATE ON tb_rbac_resources 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE IF NOT EXISTS tb_rbac_role_resources (
    role_id BIGINT,
    resource_id BIGINT,
    PRIMARY KEY (role_id, resource_id),
    FOREIGN KEY (role_id) REFERENCES tb_rbac_roles(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES tb_rbac_resources(id) ON DELETE CASCADE
);

-- 创建系统日志表
CREATE TABLE IF NOT EXISTS tb_sys_log (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) DEFAULT NULL,
    description VARCHAR(255) DEFAULT NULL,
    method VARCHAR(255) DEFAULT NULL,
    params TEXT DEFAULT NULL,
    log_type VARCHAR(255) DEFAULT NULL,
    request_ip VARCHAR(255) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    browser VARCHAR(255) DEFAULT NULL,
    user_agent VARCHAR(255) DEFAULT NULL,
    time BIGINT DEFAULT NULL,
    exception_detail TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_username ON tb_sys_log(username);
CREATE INDEX IF NOT EXISTS idx_log_type ON tb_sys_log(log_type);
CREATE INDEX IF NOT EXISTS idx_create_time ON tb_sys_log(created_at);
CREATE INDEX IF NOT EXISTS idx_request_ip ON tb_sys_log(request_ip);

CREATE TABLE IF NOT EXISTS tb_rbac_departs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    remark VARCHAR(255),
    parent_id BIGINT,
    status INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为 tb_rbac_departs 表创建更新时间触发器
DROP TRIGGER IF EXISTS update_tb_rbac_departs_updated_at ON tb_rbac_departs;
CREATE TRIGGER update_tb_rbac_departs_updated_at 
    BEFORE UPDATE ON tb_rbac_departs 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
