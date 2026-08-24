-- V46__create_attendance_schedules_policies_and_corrections.sql
-- US-A01 to US-A10: Schema for Attendance Management Module

-- ============================================================
-- 1. work_schedules: Configurable working hours per tenant
-- ============================================================
CREATE TABLE IF NOT EXISTS work_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(255) NOT NULL,
    start_time TIME NOT NULL DEFAULT '08:30:00',
    end_time TIME NOT NULL DEFAULT '17:30:00',
    grace_period_minutes INT NOT NULL DEFAULT 15,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_created_by VARCHAR(255),
    audit_updated_by VARCHAR(255),
    INDEX idx_work_schedules_tenant (tenant_id, is_active)
);

-- Seed default work schedule
INSERT INTO work_schedules (tenant_id, name, start_time, end_time, grace_period_minutes, is_active)
VALUES (1, 'Giờ hành chính tiêu chuẩn', '08:30:00', '17:30:00', 15, TRUE);

-- ============================================================
-- 2. attendance_policies: Configurable rules per tenant
-- ============================================================
CREATE TABLE IF NOT EXISTS attendance_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    required_daily_minutes INT NOT NULL DEFAULT 480,
    grace_period_minutes INT NOT NULL DEFAULT 15,
    allow_late BOOLEAN NOT NULL DEFAULT TRUE,
    allow_early_leave BOOLEAN NOT NULL DEFAULT TRUE,
    require_checkout BOOLEAN NOT NULL DEFAULT TRUE,
    max_correction_days INT NOT NULL DEFAULT 3,
    auto_mark_absent BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_created_by VARCHAR(255),
    audit_updated_by VARCHAR(255),
    UNIQUE INDEX uk_attendance_policy_tenant (tenant_id)
);

-- Seed default policy
INSERT INTO attendance_policies (tenant_id, required_daily_minutes, grace_period_minutes, max_correction_days, auto_mark_absent)
VALUES (1, 480, 15, 3, TRUE);

-- ============================================================
-- 3. Enhance attendances table
-- ============================================================
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendances' AND COLUMN_NAME = 'scheduled_start_at') > 0, 'SELECT 1', 'ALTER TABLE attendances ADD COLUMN scheduled_start_at DATETIME NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendances' AND COLUMN_NAME = 'scheduled_end_at') > 0, 'SELECT 1', 'ALTER TABLE attendances ADD COLUMN scheduled_end_at DATETIME NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendances' AND COLUMN_NAME = 'worked_minutes') > 0, 'SELECT 1', 'ALTER TABLE attendances ADD COLUMN worked_minutes INT DEFAULT 0'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendances' AND COLUMN_NAME = 'late_minutes') > 0, 'SELECT 1', 'ALTER TABLE attendances ADD COLUMN late_minutes INT DEFAULT 0'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendances' AND COLUMN_NAME = 'early_leave_minutes') > 0, 'SELECT 1', 'ALTER TABLE attendances ADD COLUMN early_leave_minutes INT DEFAULT 0'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendances' AND COLUMN_NAME = 'check_in_method') > 0, 'SELECT 1', 'ALTER TABLE attendances ADD COLUMN check_in_method VARCHAR(30) DEFAULT ''WEB'''));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendances' AND COLUMN_NAME = 'check_out_method') > 0, 'SELECT 1', 'ALTER TABLE attendances ADD COLUMN check_out_method VARCHAR(30) DEFAULT ''WEB'''));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Unique constraint: 1 attendance per intern per day per tenant
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'attendances'
    AND INDEX_NAME = 'uk_attendance_intern_date');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE attendances ADD UNIQUE INDEX uk_attendance_intern_date (tenant_id, intern_id, date)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 4. attendance_corrections: Request table for forgotten check-outs/errors
-- ============================================================
CREATE TABLE IF NOT EXISTS attendance_corrections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    attendance_id BIGINT NOT NULL,
    intern_id BIGINT NOT NULL,
    requested_check_in DATETIME NULL,
    requested_check_out DATETIME NULL,
    reason VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    review_comment VARCHAR(2000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_created_by VARCHAR(255),
    audit_updated_by VARCHAR(255),
    CONSTRAINT fk_correction_attendance FOREIGN KEY (attendance_id) REFERENCES attendances(id) ON DELETE CASCADE,
    CONSTRAINT fk_correction_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id),
    INDEX idx_correction_tenant_status (tenant_id, status),
    INDEX idx_correction_intern (intern_id)
);

-- ============================================================
-- 5. leave_types & leave_requests enhancement
-- ============================================================
CREATE TABLE IF NOT EXISTS leave_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    requires_attachment BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_leave_type_code (tenant_id, code)
);

-- Seed default leave types
INSERT INTO leave_types (tenant_id, code, name, description, is_paid, requires_attachment) VALUES
(1, 'SICK', 'Nghỉ ốm', 'Nghỉ do bị bệnh (có hoặc không có giấy xác nhận y tế)', FALSE, FALSE),
(1, 'PERSONAL', 'Nghỉ việc riêng', 'Nghỉ giải quyết công việc cá nhân', FALSE, FALSE),
(1, 'SCHOOL', 'Nghỉ lịch học / thi', 'Nghỉ để đi học hoặc thi tại trường đại học', FALSE, TRUE),
(1, 'ANNUAL', 'Nghỉ phép thường niên', 'Nghỉ phép tiêu chuẩn', TRUE, FALSE);

-- Enhance leave_requests table
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leave_requests' AND COLUMN_NAME = 'leave_type_id') > 0, 'SELECT 1', 'ALTER TABLE leave_requests ADD COLUMN leave_type_id BIGINT NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leave_requests' AND COLUMN_NAME = 'attachment_url') > 0, 'SELECT 1', 'ALTER TABLE leave_requests ADD COLUMN attachment_url VARCHAR(1000) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leave_requests' AND COLUMN_NAME = 'total_days') > 0, 'SELECT 1', 'ALTER TABLE leave_requests ADD COLUMN total_days DECIMAL(4,1) DEFAULT 1.0'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'leave_requests' AND COLUMN_NAME = 'reviewed_at') > 0, 'SELECT 1', 'ALTER TABLE leave_requests ADD COLUMN reviewed_at DATETIME NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
