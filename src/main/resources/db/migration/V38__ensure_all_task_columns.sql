/**
 * V38__ensure_all_task_columns.sql
 * Ensure all required columns for Task Entity exist in tasks table
 */

-- 1. Make group_id nullable
ALTER TABLE tasks MODIFY COLUMN group_id BIGINT NULL;

-- 2. tenant_id
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'tenant_id') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. intern_id
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'intern_id') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN intern_id BIGINT NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. mentor_id
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'mentor_id') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN mentor_id BIGINT NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. priority
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'priority') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT ''MEDIUM'''));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. progress
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'progress') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN progress TINYINT UNSIGNED NOT NULL DEFAULT 0'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. start_date
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'start_date') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN start_date DATETIME NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 8. completed_at
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'completed_at') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN completed_at DATETIME NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 9. weight
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'weight') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN weight INT NOT NULL DEFAULT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 10. version
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'version') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN version BIGINT NOT NULL DEFAULT 0'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 11. audit_created_by
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'audit_created_by') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN audit_created_by VARCHAR(255) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 12. audit_updated_by
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND COLUMN_NAME = 'audit_updated_by') > 0, 'SELECT 1', 'ALTER TABLE tasks ADD COLUMN audit_updated_by VARCHAR(255) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 13. Ensure task_progress_histories table
CREATE TABLE IF NOT EXISTS task_progress_histories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    task_id BIGINT NOT NULL,
    old_progress TINYINT UNSIGNED NULL,
    new_progress TINYINT UNSIGNED NOT NULL,
    old_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NULL,
    note VARCHAR(1000) NULL,
    changed_by BIGINT NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,
    INDEX idx_prog_hist_task (task_id),
    INDEX idx_prog_hist_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
