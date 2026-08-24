/**
 * V40__add_audit_columns_to_task_progress_histories.sql
 * Add missing audit columns to task_progress_histories table
 */

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'task_progress_histories' 
     AND COLUMN_NAME = 'audit_created_by') > 0,
    'SELECT 1',
    'ALTER TABLE task_progress_histories ADD COLUMN audit_created_by VARCHAR(255) NULL'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'task_progress_histories' 
     AND COLUMN_NAME = 'audit_updated_by') > 0,
    'SELECT 1',
    'ALTER TABLE task_progress_histories ADD COLUMN audit_updated_by VARCHAR(255) NULL'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
