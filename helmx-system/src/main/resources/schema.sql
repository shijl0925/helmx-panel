CREATE TABLE IF NOT EXISTS tb_users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
                                     password VARCHAR(255) NOT NULL,
                                     nickname VARCHAR(50),
                                     phone VARCHAR(20),
                                     email VARCHAR(100) NOT NULL,
                                     status INT DEFAULT 1,
                                     is_super_admin BOOLEAN DEFAULT FALSE,
                                     depart_id BIGINT COMMENT '所属部门ID',
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_rbac_roles (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     name VARCHAR(50) NOT NULL UNIQUE,
                                     remark VARCHAR(255),
                                     status INT DEFAULT 1,
                                     code VARCHAR(50) UNIQUE,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_rbac_user_roles (
                                          user_id BIGINT,
                                          role_id BIGINT,
                                          PRIMARY KEY (user_id, role_id),
                                          FOREIGN KEY (user_id) REFERENCES tb_users(id) ON DELETE CASCADE,
                                          FOREIGN KEY (role_id) REFERENCES tb_rbac_roles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tb_rbac_menus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    parent_id BIGINT,
    type VARCHAR(32) NOT NULL,
    auth_code VARCHAR(64),
    path VARCHAR(64),
    component VARCHAR(64),
    status INT DEFAULT 1,
    active_path VARCHAR(64),
    icon VARCHAR(64),
    `order` INT,
    title VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_rbac_role_menus (
                                          role_id BIGINT,
                                          menu_id BIGINT,
                                          PRIMARY KEY (role_id, menu_id),
                                          FOREIGN KEY (role_id) REFERENCES tb_rbac_roles(id) ON DELETE CASCADE,
                                          FOREIGN KEY (menu_id) REFERENCES tb_rbac_menus(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tb_rbac_resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    parent_id BIGINT,
    status INT DEFAULT 1,
    type VARCHAR(32) NOT NULL,
    path VARCHAR(64) NOT NULL,
    method VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_rbac_role_resources (
                                          role_id BIGINT,
                                          resource_id BIGINT,
                                          PRIMARY KEY (role_id, resource_id),
                                          FOREIGN KEY (role_id) REFERENCES tb_rbac_roles(id) ON DELETE CASCADE,
                                          FOREIGN KEY (resource_id) REFERENCES tb_rbac_resources(id) ON DELETE CASCADE
);

-- 创建系统日志表
CREATE TABLE IF NOT EXISTS `tb_sys_log` (
                                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                            `username` VARCHAR(255) DEFAULT NULL COMMENT '操作用户',
                                            `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
                                            `method` VARCHAR(255) DEFAULT NULL COMMENT '方法名',
                                            `params` TEXT DEFAULT NULL COMMENT '参数',
                                            `log_type` VARCHAR(255) DEFAULT NULL COMMENT '日志类型',
                                            `request_ip` VARCHAR(255) DEFAULT NULL COMMENT '请求ip',
                                            `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
                                            `browser` VARCHAR(255) DEFAULT NULL COMMENT '浏览器',
                                            `user_agent` VARCHAR(255) DEFAULT NULL COMMENT 'User Agent',
                                            `time` BIGINT DEFAULT NULL COMMENT '请求耗时',
                                            `exception_detail` TEXT DEFAULT NULL COMMENT '异常详细',
                                            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
                                            PRIMARY KEY (`id`),
                                            INDEX `idx_username` (`username`),
                                            INDEX `idx_log_type` (`log_type`),
                                            INDEX `idx_create_time` (`created_at`),
                                            INDEX `idx_request_ip` (`request_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

CREATE TABLE IF NOT EXISTS tb_rbac_departs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    remark VARCHAR(255),
    parent_id BIGINT,
    status INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
