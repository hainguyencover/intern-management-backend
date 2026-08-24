CREATE TABLE integration_sync_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    sync_job_id BIGINT NOT NULL,

    external_id VARCHAR(255) NOT NULL,

    internal_id BIGINT NULL,

    entity_type VARCHAR(100) NOT NULL,

    action VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    error_code VARCHAR(100) NULL,
    error_message TEXT NULL,

    processed_at DATETIME NULL,

    created_at DATETIME NOT NULL,

    CONSTRAINT fk_sync_item_job
        FOREIGN KEY (sync_job_id)
        REFERENCES integration_sync_jobs(id)
        ON DELETE CASCADE
);
