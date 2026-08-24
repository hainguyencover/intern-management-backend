-- V60__upgrade_iam_rbac_schema.sql

-- 1. Add security_version to users table
ALTER TABLE users ADD COLUMN security_version INT NOT NULL DEFAULT 1;

-- 2. Create account_activation_tokens table
CREATE TABLE account_activation_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_activation_token_hash UNIQUE (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Insert standardized RBAC permissions if not existing
INSERT IGNORE INTO permissions (code, name, description) VALUES
('USER_READ', 'Xem thông tin tài khoản', 'Quyền xem danh sách và chi tiết người dùng'),
('USER_CREATE', 'Tạo tài khoản mới', 'Quyền tạo mới tài khoản người dùng'),
('USER_UPDATE', 'Cập nhật tài khoản', 'Quyền chỉnh sửa thông tin người dùng'),
('USER_DISABLE', 'Vô hiệu hóa tài khoản', 'Quyền khóa hoặc đình chỉ tài khoản'),
('ROLE_READ', 'Xem danh sách vai trò', 'Quyền xem vai trò và nhóm quyền'),
('ROLE_UPDATE', 'Cập nhật phân quyền', 'Quyền thay đổi permissions của Role'),
('PERMISSION_READ', 'Xem danh sách quyền', 'Quyền xem Ma trận phân quyền'),
('PERMISSION_UPDATE', 'Cập nhật ma trận quyền', 'Quyền quản lý danh mục Permissions'),
('INTERN_READ', 'Xem hồ sơ thực tập sinh', 'Quyền xem thông tin TTS'),
('INTERN_UPDATE', 'Cập nhật hồ sơ TTS', 'Quyền chỉnh sửa thông tin TTS'),
('MENTOR_READ', 'Xem thông tin Mentor', 'Quyền xem danh sách Mentor'),
('TASK_CREATE', 'Tạo công việc', 'Quyền giao công việc cho TTS'),
('TASK_UPDATE', 'Cập nhật công việc', 'Quyền cập nhật tiến độ công việc'),
('ATTENDANCE_READ', 'Xem dữ liệu chấm công', 'Quyền xem nhật ký chấm công'),
('REPORT_READ', 'Xem báo cáo', 'Quyền xem tổng hợp báo cáo'),
('REPORT_EXPORT', 'Xuất báo cáo Excel', 'Quyền xuất file báo cáo');

-- 4. Map default permissions to ADMIN role (role_id = 1 or code = 'ADMIN')
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN';

-- 5. Map HR default permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('INTERN_READ', 'INTERN_UPDATE', 'MENTOR_READ', 'ATTENDANCE_READ', 'REPORT_READ', 'REPORT_EXPORT')
WHERE r.code = 'HR';

-- 6. Map MENTOR default permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('INTERN_READ', 'TASK_CREATE', 'TASK_UPDATE', 'ATTENDANCE_READ', 'REPORT_READ')
WHERE r.code = 'MENTOR';

-- 7. Map INTERN default permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('INTERN_READ', 'TASK_UPDATE', 'ATTENDANCE_READ', 'REPORT_READ')
WHERE r.code = 'INTERN';
