-- V63__add_terminated_status_and_university_notification_schema.sql

-- 1. Add university_id to notifications table for university-level querying
SET @exist_col := (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'notifications' 
      AND COLUMN_NAME = 'university_id'
);

SET @sql := IF(@exist_col = 0, 
    'ALTER TABLE notifications ADD COLUMN university_id BIGINT NULL, ADD CONSTRAINT fk_notifications_university FOREIGN KEY (university_id) REFERENCES universities(id) ON DELETE CASCADE', 
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add index on notifications(university_id, created_at)
SET @exist_idx := (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'notifications' 
      AND INDEX_NAME = 'idx_notification_university'
);

SET @sql := IF(@exist_idx = 0, 
    'CREATE INDEX idx_notification_university ON notifications(university_id, created_at)', 
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Create university_notification_preferences table
CREATE TABLE IF NOT EXISTS university_notification_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    university_id BIGINT NOT NULL,
    internship_completed_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    internship_terminated_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_univ_notification_pref UNIQUE (university_id),
    CONSTRAINT fk_univ_notification_pref_univ FOREIGN KEY (university_id) REFERENCES universities(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed default preferences for existing universities
INSERT IGNORE INTO university_notification_preferences (university_id, internship_completed_enabled, internship_terminated_enabled, email_enabled, in_app_enabled)
SELECT id, TRUE, TRUE, TRUE, TRUE FROM universities;

-- 3. Add TERMINATED permission for University notifications
INSERT IGNORE INTO permissions (code, name, description) VALUES
('UNIVERSITY_NOTIFICATION_READ', 'Xem thông báo trường đại học', 'Quyền xem danh sách và chi tiết thông báo trường'),
('UNIVERSITY_NOTIFICATION_MANAGE', 'Quản lý thông báo trường đại học', 'Quyền đánh dấu đã đọc và tùy chỉnh cấu hình nhận thông báo trường');

-- Map permissions to roles
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'UNIVERSITY_ADMIN') AND p.code LIKE 'UNIVERSITY_NOTIFICATION_%';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'UNIVERSITY_NOTIFICATION_READ'
WHERE r.code = 'UNIVERSITY_VIEWER';
