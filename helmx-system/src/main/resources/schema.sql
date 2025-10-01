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

CREATE TABLE IF NOT EXISTS tb_docker_env (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    remark VARCHAR(255),
    host VARCHAR(64) NOT NULL,
    status INT DEFAULT 1,
    tls_verify BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_docker_registry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    url VARCHAR(255) NOT NULL,
    username VARCHAR(64),
    password VARCHAR(64),
    auth BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);