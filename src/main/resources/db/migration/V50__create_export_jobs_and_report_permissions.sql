-- V50__create_export_jobs_and_report_permissions.sql

CREATE TABLE IF NOT EXISTS export_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    job_code VARCHAR(100) NOT NULL,
    report_code VARCHAR(100) NOT NULL,
    format VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    filters JSON NULL,
    file_name VARCHAR(255) NULL,
    file_path VARCHAR(500) NULL,
    file_size BIGINT NULL,
    record_count INT NULL DEFAULT 0,
    error_message TEXT NULL,
    requested_by BIGINT NOT NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_export_jobs_job_code UNIQUE (tenant_id, job_code),
    INDEX idx_export_jobs_tenant (tenant_id),
    INDEX idx_export_jobs_status (tenant_id, status),
    INDEX idx_export_jobs_requested_by (tenant_id, requested_by),
    INDEX idx_export_jobs_created_at (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed report permissions
INSERT INTO permissions (code, name, description)
SELECT 'REPORT_VIEW', 'Báo cáo - Xem báo cáo', 'Quyền xem danh mục và xem trước dữ liệu báo cáo'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'REPORT_VIEW');

INSERT INTO permissions (code, name, description)
SELECT 'REPORT_EXPORT', 'Báo cáo - Xuất báo cáo', 'Quyền xuất dữ liệu báo cáo'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'REPORT_EXPORT');

INSERT INTO permissions (code, name, description)
SELECT 'REPORT_EXPORT_EXCEL', 'Báo cáo - Xuất Excel', 'Quyền xuất báo cáo dưới dạng Excel'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'REPORT_EXPORT_EXCEL');

INSERT INTO permissions (code, name, description)
SELECT 'REPORT_EXPORT_PDF', 'Báo cáo - Xuất PDF', 'Quyền xuất báo cáo dưới dạng PDF'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'REPORT_EXPORT_PDF');

INSERT INTO permissions (code, name, description)
SELECT 'REPORT_EXPORT_HISTORY', 'Báo cáo - Xem lịch sử xuất', 'Quyền xem lịch sử xuất báo cáo'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'REPORT_EXPORT_HISTORY');

-- Assign report permissions to ADMIN and HR roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ROLE_ADMIN', 'HR', 'ROLE_HR')
  AND p.code IN ('REPORT_VIEW', 'REPORT_EXPORT', 'REPORT_EXPORT_EXCEL', 'REPORT_EXPORT_PDF', 'REPORT_EXPORT_HISTORY')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
