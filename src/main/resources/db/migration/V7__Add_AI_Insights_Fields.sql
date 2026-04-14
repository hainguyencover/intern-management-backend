-- Migration for Phase 13 AI Integration

-- Create weekly_reports table (not included in V1 init)
CREATE TABLE IF NOT EXISTS weekly_reports (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    intern_id        BIGINT       NOT NULL,
    week_number      INT          NOT NULL,
    title            VARCHAR(255) NOT NULL,
    week_start       DATE         NOT NULL,
    week_end         DATE         NOT NULL,
    report_date      DATE         NOT NULL,
    completed_work   TEXT         NOT NULL,
    planned_work     TEXT         NULL,
    challenges       TEXT         NULL,
    learnings        TEXT         NULL,
    file_url         VARCHAR(255) NULL DEFAULT '',
    mentor_feedback  TEXT         NULL,
    rating           INT          NULL,
    mentor_id        BIGINT       NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    submitted_at     DATETIME     NULL,
    reviewed_at      DATETIME     NULL,
    sentiment_label  VARCHAR(20)  NULL,
    sentiment_score  DOUBLE       NULL,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_weekly_reports_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_weekly_reports_mentor FOREIGN KEY (mentor_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_reports_intern_week (intern_id, week_number),
    INDEX idx_reports_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE intern_profiles
ADD COLUMN cv_skills VARCHAR(1000),
ADD COLUMN cv_score INT,
ADD COLUMN cv_summary TEXT;
