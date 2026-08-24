-- V45__create_final_evaluation_reports.sql
-- US-020 Phase 3: Final Evaluation Reports schema for immutable snapshot-based reporting

CREATE TABLE IF NOT EXISTS final_evaluation_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    intern_id BIGINT NOT NULL,
    program_id BIGINT NULL,
    mentor_id BIGINT NULL,
    report_number VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    
    -- Snapshot scores (scale 0 - 10)
    evaluation_score DECIMAL(5,2),
    task_score DECIMAL(5,2),
    attendance_score DECIMAL(5,2),
    weekly_report_score DECIMAL(5,2),
    final_score DECIMAL(5,2),
    classification VARCHAR(30),
    
    -- Snapshot performance stats
    task_total INT DEFAULT 0,
    task_completed INT DEFAULT 0,
    task_overdue INT DEFAULT 0,
    task_completion_rate DECIMAL(5,2),
    
    attendance_total INT DEFAULT 0,
    attendance_present INT DEFAULT 0,
    attendance_absent INT DEFAULT 0,
    attendance_late INT DEFAULT 0,
    attendance_rate DECIMAL(5,2),
    
    report_total INT DEFAULT 0,
    report_submitted INT DEFAULT 0,
    report_late INT DEFAULT 0,
    report_missing INT DEFAULT 0,
    
    -- Comments
    mentor_comment VARCHAR(4000),
    hr_comment VARCHAR(4000),
    return_reason VARCHAR(2000),
    
    -- Audit timestamps
    generated_at DATETIME,
    approved_at DATETIME,
    published_at DATETIME,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_created_by VARCHAR(255),
    audit_updated_by VARCHAR(255),
    
    CONSTRAINT fk_final_report_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id),
    UNIQUE INDEX uk_final_report_intern (tenant_id, intern_id),
    INDEX idx_final_report_status (tenant_id, status),
    INDEX idx_final_report_program (tenant_id, program_id)
);

CREATE TABLE IF NOT EXISTS final_report_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    item_key VARCHAR(100) NOT NULL,
    item_value VARCHAR(500),
    score DECIMAL(5,2),
    display_order INT DEFAULT 0,
    CONSTRAINT fk_report_item_report FOREIGN KEY (report_id) REFERENCES final_evaluation_reports(id) ON DELETE CASCADE,
    INDEX idx_report_item (report_id)
);
