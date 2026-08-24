/**
 * V36__us015_us016_task_management_and_progress.sql
 * Schema enhancements for Task Management (US-015) and Progress Tracking (US-016)
 */

-- 1. Upgrade tasks table columns
ALTER TABLE tasks ADD COLUMN intern_id BIGINT NULL AFTER group_id;
ALTER TABLE tasks ADD COLUMN mentor_id BIGINT NULL AFTER intern_id;
ALTER TABLE tasks ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' AFTER description;
ALTER TABLE tasks ADD COLUMN progress TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER status;
ALTER TABLE tasks ADD COLUMN start_date DATETIME NULL AFTER progress;
ALTER TABLE tasks ADD COLUMN completed_at DATETIME NULL AFTER due_date;
ALTER TABLE tasks ADD COLUMN weight INT NOT NULL DEFAULT 1 AFTER completed_at;
ALTER TABLE tasks ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER updated_at;

-- 2. Add foreign keys
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id) ON DELETE CASCADE;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_mentor FOREIGN KEY (mentor_id) REFERENCES users(id) ON DELETE SET NULL;

-- 3. Add indexes
CREATE INDEX idx_tasks_intern ON tasks(intern_id);
CREATE INDEX idx_tasks_mentor ON tasks(mentor_id);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_intern_status ON tasks(intern_id, status);
CREATE INDEX idx_tasks_mentor_status ON tasks(mentor_id, status);

-- 4. Create task_progress_histories table
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
    CONSTRAINT fk_progress_hist_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_progress_hist_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_hist_user FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_prog_hist_task (task_id),
    INDEX idx_prog_hist_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
