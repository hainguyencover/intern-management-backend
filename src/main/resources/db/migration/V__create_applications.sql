CREATE TABLE IF NOT EXISTS applications (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            intern_id BIGINT NOT NULL,
                                            position VARCHAR(100) NOT NULL,
    applied_at DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL,
    note TEXT NULL,
    CONSTRAINT fk_app_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id)
    );

CREATE INDEX idx_app_status_applied ON applications(status, applied_at);
