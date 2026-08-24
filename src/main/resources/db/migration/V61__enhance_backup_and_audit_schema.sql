-- =====================================================
-- Migration V61: Enhance backup_jobs & audit_logs schema + RBAC permissions
-- =====================================================

-- 1. Enhance backup_jobs table
ALTER TABLE backup_jobs
    ADD COLUMN tenant_id BIGINT NULL AFTER id,
    ADD COLUMN backup_type VARCHAR(30) NOT NULL DEFAULT 'FULL' AFTER tenant_id,
    ADD COLUMN storage_path VARCHAR(1000) NULL AFTER file_path,
    ADD COLUMN retention_until DATETIME NULL AFTER finished_at,
    ADD COLUMN error_code VARCHAR(100) NULL AFTER message,
    ADD COLUMN error_message VARCHAR(1000) NULL AFTER error_code,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

-- Indexes for backup_jobs
CREATE INDEX idx_backup_status ON backup_jobs(status);
CREATE INDEX idx_backup_started_at ON backup_jobs(started_at);
CREATE INDEX idx_backup_retention ON backup_jobs(retention_until);

-- 2. Enhance audit_logs table
ALTER TABLE audit_logs
    ADD COLUMN tenant_id BIGINT NULL AFTER id,
    ADD COLUMN actor_username VARCHAR(255) NULL AFTER actor_id,
    ADD COLUMN actor_role VARCHAR(100) NULL AFTER actor_email,
    ADD COLUMN resource_type VARCHAR(100) NULL AFTER action,
    ADD COLUMN resource_id VARCHAR(100) NULL AFTER resource_type,
    ADD COLUMN result VARCHAR(30) NOT NULL DEFAULT 'SUCCESS' AFTER resource_id,
    ADD COLUMN old_value JSON NULL AFTER before_json,
    ADD COLUMN new_value JSON NULL AFTER old_value,
    ADD COLUMN metadata JSON NULL AFTER new_value;

-- Composite indexes for audit_logs performance
CREATE INDEX idx_audit_tenant_created ON audit_logs(tenant_id, created_at);
CREATE INDEX idx_audit_actor_created ON audit_logs(actor_id, created_at);
CREATE INDEX idx_audit_action_created ON audit_logs(action, created_at);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_request ON audit_logs(request_id);

-- 3. Add Backup & Audit RBAC Permissions
INSERT IGNORE INTO permissions (code, name, module, description) VALUES
('BACKUP_CREATE', 'Tạo bản sao lưu dữ liệu', 'SYSTEM', 'Quyền thực hiện backup dữ liệu hệ thống thủ công'),
('BACKUP_RESTORE', 'Khôi phục dữ liệu hệ thống', 'SYSTEM', 'Quyền khôi phục dữ liệu từ bản sao lưu'),
('BACKUP_DOWNLOAD', 'Tải bản sao lưu', 'SYSTEM', 'Quyền tải bản sao lưu về máy local'),
('AUDIT_LOG_READ', 'Xem nhật ký hệ thống', 'SYSTEM', 'Quyền xem danh sách và chi tiết Audit Log'),
('AUDIT_LOG_EXPORT', 'Xuất file nhật ký hệ thống', 'SYSTEM', 'Quyền xuất file báo cáo Audit Log');

-- Map new permissions to ADMIN role
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN ('BACKUP_CREATE', 'BACKUP_RESTORE', 'BACKUP_DOWNLOAD', 'AUDIT_LOG_READ', 'AUDIT_LOG_EXPORT', 'BACKUP_READ', 'AUDIT_READ');
