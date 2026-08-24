CREATE TABLE integration_connections (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    tenant_id BIGINT NOT NULL,

    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,

    integration_type VARCHAR(50) NOT NULL,
    provider VARCHAR(100) NOT NULL,

    base_url VARCHAR(500),

    status VARCHAR(30) NOT NULL DEFAULT 'INACTIVE',

    auth_type VARCHAR(30),

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    last_sync_at DATETIME NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    audit_created_by BIGINT NULL,
    audit_updated_by BIGINT NULL,

    CONSTRAINT uk_integration_connection
        UNIQUE (tenant_id, code)
);
