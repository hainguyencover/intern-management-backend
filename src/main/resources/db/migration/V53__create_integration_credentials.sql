CREATE TABLE integration_credentials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    connection_id BIGINT NOT NULL,

    credential_key VARCHAR(100) NOT NULL,

    encrypted_value TEXT NOT NULL,

    expires_at DATETIME NULL,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_integration_credentials_connection
        FOREIGN KEY (connection_id)
        REFERENCES integration_connections(id)
        ON DELETE CASCADE
);
