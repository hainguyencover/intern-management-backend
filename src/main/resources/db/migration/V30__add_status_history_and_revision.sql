-- V30: Create application_status_history table for US-051 Audit Trail
CREATE TABLE IF NOT EXISTS application_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    changed_by_id BIGINT,
    reason TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_status_hist_app FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_status_hist_user FOREIGN KEY (changed_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_app_status_hist_app ON application_status_history(application_id);
CREATE INDEX idx_app_status_hist_created ON application_status_history(created_at);
