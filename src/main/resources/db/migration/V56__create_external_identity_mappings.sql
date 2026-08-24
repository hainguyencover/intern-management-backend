CREATE TABLE external_identity_mappings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    tenant_id BIGINT NOT NULL,

    system_code VARCHAR(100) NOT NULL,

    entity_type VARCHAR(100) NOT NULL,

    external_id VARCHAR(255) NOT NULL,

    internal_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT uk_external_identity
        UNIQUE (
            tenant_id,
            system_code,
            entity_type,
            external_id
        )
);
