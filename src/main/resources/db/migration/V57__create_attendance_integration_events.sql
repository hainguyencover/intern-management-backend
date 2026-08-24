CREATE TABLE attendance_integration_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    tenant_id BIGINT NOT NULL,

    connection_id BIGINT NOT NULL,

    external_event_id VARCHAR(255) NOT NULL,

    external_employee_id VARCHAR(255) NOT NULL,

    event_type VARCHAR(30) NOT NULL,

    event_time DATETIME NOT NULL,

    device_id VARCHAR(255),

    method VARCHAR(30),

    raw_payload JSON,

    processing_status VARCHAR(30) NOT NULL,

    processed_at DATETIME NULL,

    error_message TEXT NULL,

    created_at DATETIME NOT NULL,

    CONSTRAINT uk_attendance_external_event
        UNIQUE (
            tenant_id,
            connection_id,
            external_event_id
        )
);
