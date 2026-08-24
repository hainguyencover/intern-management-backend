/**
 * V37__fix_tasks_schema_for_jpa.sql
 * Make group_id column nullable on tasks table and ensure task_progress_histories table exists
 */
ALTER TABLE tasks MODIFY COLUMN group_id BIGINT NULL;

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
    CONSTRAINT fk_progress_hist_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_progress_hist_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_hist_user FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_prog_hist_task (task_id),
    INDEX idx_prog_hist_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
