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

CREATE TABLE IF NOT EXISTS tb_docker_env (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    remark VARCHAR(255),
    status INTEGER DEFAULT 1,
    host VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为 tb_docker_env 表创建更新时间触发器
DROP TRIGGER IF EXISTS update_tb_docker_env_updated_at ON tb_docker_env;
CREATE TRIGGER update_tb_docker_env_updated_at
    BEFORE UPDATE ON tb_docker_env
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE IF NOT EXISTS tb_docker_registry (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    url VARCHAR(255) NOT NULL,
    username VARCHAR(50),
    password VARCHAR(50),
    auth BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为 tb_docker_registry 表创建更新时间触发器
DROP TRIGGER IF EXISTS update_tb_docker_registry_updated_at ON tb_docker_registry;
CREATE TRIGGER update_tb_docker_registry_updated_at
    BEFORE UPDATE ON tb_docker_registry
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();