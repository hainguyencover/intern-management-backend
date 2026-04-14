-- V8: Create missing tables and add missing columns

-- Users: Add 2FA and address columns
ALTER TABLE users ADD COLUMN two_factor_secret VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN is_two_factor_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN address VARCHAR(100) NULL;

-- Departments: Add description column
ALTER TABLE departments ADD COLUMN description TEXT NULL;

-- Intern profiles: Add mentor_id FK
ALTER TABLE intern_profiles ADD COLUMN mentor_id BIGINT NULL;
ALTER TABLE intern_profiles ADD CONSTRAINT fk_intern_profiles_mentor FOREIGN KEY (mentor_id) REFERENCES mentors(id) ON DELETE SET NULL;

-- Create refresh_tokens table
CREATE TABLE refresh_tokens (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NULL,
    token       VARCHAR(255) NOT NULL,
    expiry_date DATETIME     NOT NULL,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create system_configs table
CREATE TABLE system_configs (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key   VARCHAR(255) NOT NULL,
    config_value VARCHAR(255) NULL,
    description  VARCHAR(500) NULL,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_system_configs_key UNIQUE (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create leave_requests table
CREATE TABLE leave_requests (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    intern_id       BIGINT      NOT NULL,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    reason          TEXT        NOT NULL,
    leave_type      VARCHAR(20) NULL,
    status          VARCHAR(20) NOT NULL,
    approved_by     BIGINT      NULL,
    rejected_reason TEXT        NULL,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_requests_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_requests_approved_by FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create allowances table
CREATE TABLE allowances (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    intern_id       BIGINT         NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    allowance_month DATE           NOT NULL,
    payment_date    DATE           NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    notes           VARCHAR(1000)  NULL,
    paid_by         BIGINT         NULL,
    audit_created_by VARCHAR(255)  NULL,
    audit_updated_by VARCHAR(255)  NULL,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_allowances_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_allowances_paid_by FOREIGN KEY (paid_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_allowances_intern (intern_id),
    INDEX idx_allowances_month (allowance_month),
    INDEX idx_allowances_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
