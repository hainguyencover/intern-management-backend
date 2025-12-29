-- V2__rbac_permissions.sql

CREATE TABLE permissions
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_permissions_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_permissions
(
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


