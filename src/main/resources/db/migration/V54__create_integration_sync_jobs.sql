CREATE TABLE integration_sync_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    tenant_id BIGINT NOT NULL,

    connection_id BIGINT NOT NULL,

    sync_type VARCHAR(50) NOT NULL,

    direction VARCHAR(20) NOT NULL,

    status VARCHAR(30) NOT NULL,

    started_at DATETIME NULL,
    completed_at DATETIME NULL,

    total_records INT DEFAULT 0,
    success_records INT DEFAULT 0,
    failed_records INT DEFAULT 0,

    error_message TEXT NULL,

    retry_count INT DEFAULT 0,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_sync_job_connection
        FOREIGN KEY (connection_id)
        REFERENCES integration_connections(id)
        ON DELETE CASCADE
);
