/**
 * V39__fix_tasks_and_history_fk_constraints.sql
 * Drop strict foreign key constraints on task_progress_histories and tasks to prevent UnexpectedRollbackException
 */

-- 1. Ensure columns are nullable on tasks
ALTER TABLE tasks MODIFY COLUMN group_id BIGINT NULL;
ALTER TABLE tasks MODIFY COLUMN mentor_id BIGINT NULL;
ALTER TABLE tasks MODIFY COLUMN intern_id BIGINT NULL;

-- 2. Drop strict tenant FK on task_progress_histories if exists
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
     WHERE CONSTRAINT_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'task_progress_histories' 
     AND CONSTRAINT_NAME = 'fk_progress_hist_tenant') > 0,
    'ALTER TABLE task_progress_histories DROP FOREIGN KEY fk_progress_hist_tenant',
    'SELECT 1'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Drop mentor FK on tasks if exists to prevent mismatch between mentors table and users table
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
     WHERE CONSTRAINT_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'tasks' 
     AND CONSTRAINT_NAME = 'fk_tasks_mentor') > 0,
    'ALTER TABLE tasks DROP FOREIGN KEY fk_tasks_mentor',
    'SELECT 1'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
