-- V41__create_weekly_reports_and_feedbacks.sql
-- US-017 & US-018: Weekly Reports & Mentor Feedbacks

CREATE TABLE IF NOT EXISTS weekly_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    intern_id BIGINT NOT NULL,
    program_id BIGINT NULL,
    mentor_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    title VARCHAR(255) NULL,
    work_summary TEXT NOT NULL,
    achievements TEXT NULL,
    challenges TEXT NULL,
    next_week_plan TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME NULL,
    is_late TINYINT(1) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,

    CONSTRAINT uk_weekly_report_intern_week UNIQUE (tenant_id, intern_id, week_start_date),
    CONSTRAINT fk_weekly_report_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_weekly_report_mentor FOREIGN KEY (mentor_id) REFERENCES mentors (id) ON DELETE RESTRICT,
    CONSTRAINT fk_weekly_report_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE SET NULL,

    INDEX idx_weekly_report_intern (tenant_id, intern_id),
    INDEX idx_weekly_report_mentor (tenant_id, mentor_id),
    INDEX idx_weekly_report_status (tenant_id, status),
    INDEX idx_weekly_report_week (tenant_id, week_start_date, week_end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS weekly_report_feedbacks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    weekly_report_id BIGINT NOT NULL,
    mentor_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,

    CONSTRAINT fk_feedback_report FOREIGN KEY (weekly_report_id) REFERENCES weekly_reports (id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_mentor FOREIGN KEY (mentor_id) REFERENCES mentors (id) ON DELETE RESTRICT,

    INDEX idx_feedback_report (weekly_report_id),
    INDEX idx_feedback_mentor (tenant_id, mentor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
