-- =====================================================
-- Migration V5: RBAC chi tiết + Audit logs + Backup jobs
-- =====================================================

-- 1. Bảng permissions (đã có entity Permission.java)
-- Bổ sung thêm cột module để nhóm quyền
ALTER TABLE permissions ADD COLUMN module VARCHAR(50);

-- 2. Bảng role_permissions (many-to-many)
CREATE TABLE IF NOT EXISTS role_permissions (
                                                role_id BIGINT NOT NULL,
                                                permission_id BIGINT NOT NULL,
                                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Bảng user_permissions (optional, cho override permissions của user)
CREATE TABLE IF NOT EXISTS user_permissions (
                                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                user_id BIGINT NOT NULL,
                                                permission_id BIGINT NOT NULL,
                                                mode ENUM('GRANT', 'REVOKE') DEFAULT 'GRANT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_permission (user_id, permission_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Bảng audit_logs
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          actor_id BIGINT,
                                          actor_email VARCHAR(255),
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64),
    entity_id BIGINT,
    status VARCHAR(16) DEFAULT 'SUCCESS',
    ip_address VARCHAR(64),
    user_agent VARCHAR(255),
    request_id VARCHAR(64),
    message TEXT,
    before_json JSON,
    after_json JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_time (created_at),
    INDEX idx_audit_actor (actor_id, created_at),
    INDEX idx_audit_action (action, created_at),
    INDEX idx_audit_entity (entity_type, entity_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Bảng backup_jobs
CREATE TABLE IF NOT EXISTS backup_jobs (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           type VARCHAR(32) DEFAULT 'MANUAL',
    status VARCHAR(16) DEFAULT 'RUNNING',
    file_path VARCHAR(512),
    file_size BIGINT,
    checksum VARCHAR(128),
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    message TEXT,
    created_by BIGINT,
    INDEX idx_backup_status (status, started_at),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- SEED DATA: Permissions
-- =====================================================

-- Admin/User permissions
INSERT IGNORE INTO permissions (code, name, module, description) VALUES
('USER_CREATE', 'Tạo người dùng', 'USER', 'Quyền tạo tài khoản user mới'),
('USER_READ', 'Xem người dùng', 'USER', 'Quyền xem danh sách và chi tiết user'),
('USER_UPDATE', 'Cập nhật người dùng', 'USER', 'Quyền chỉnh sửa thông tin user'),
('USER_DELETE', 'Xóa người dùng', 'USER', 'Quyền xóa user'),
('USER_LOCK', 'Khóa/Mở khóa người dùng', 'USER', 'Quyền khóa hoặc mở khóa tài khoản'),
('USER_RESET_PASSWORD', 'Reset mật khẩu', 'USER', 'Quyền reset mật khẩu user');

-- Role & Permission management
INSERT IGNORE INTO permissions (code, name, module, description) VALUES
('ROLE_READ', 'Xem vai trò', 'ROLE', 'Quyền xem danh sách roles'),
('ROLE_UPDATE', 'Cập nhật vai trò', 'ROLE', 'Quyền chỉnh sửa roles'),
('PERMISSION_READ', 'Xem quyền', 'PERMISSION', 'Quyền xem danh sách permissions'),
('PERMISSION_UPDATE', 'Cập nhật quyền', 'PERMISSION', 'Quyền gán permissions cho roles');

-- System permissions
INSERT IGNORE INTO permissions (code, name, module, description) VALUES
('BACKUP_RUN', 'Chạy backup', 'SYSTEM', 'Quyền thực hiện backup thủ công'),
('BACKUP_READ', 'Xem lịch sử backup', 'SYSTEM', 'Quyền xem danh sách backup jobs'),
('AUDIT_READ', 'Xem audit logs', 'SYSTEM', 'Quyền xem nhật ký hoạt động hệ thống');

-- Intern management permissions
INSERT IGNORE INTO permissions (code, name, module, description) VALUES
('INTERN_READ', 'Xem thực tập sinh', 'INTERN', 'Quyền xem danh sách interns'),
('INTERN_WRITE', 'Quản lý thực tập sinh', 'INTERN', 'Quyền tạo/sửa interns'),
('APPLICATION_REVIEW', 'Duyệt hồ sơ', 'APPLICATION', 'Quyền duyệt/từ chối applications'),
('TASK_ASSIGN', 'Giao việc', 'TASK', 'Quyền giao task cho interns'),
('EVALUATION_WRITE', 'Đánh giá', 'EVALUATION', 'Quyền tạo đánh giá cho interns');

-- =====================================================
-- SEED DATA: Gán permissions cho roles mặc định
-- =====================================================

-- ADMIN: Full permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.code = 'ADMIN';

-- HR: User management + Intern management + Applications
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.code = 'HR'
  AND p.code IN (
                 'USER_READ', 'USER_CREATE', 'USER_UPDATE',
                 'INTERN_READ', 'INTERN_WRITE',
                 'APPLICATION_REVIEW',
                 'BACKUP_READ'
    );

-- MENTOR: Task & Evaluation
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.code = 'MENTOR'
  AND p.code IN (
                 'INTERN_READ',
                 'TASK_ASSIGN',
                 'EVALUATION_WRITE'
    );

-- INTERN: Self-service only (có thể bổ sung sau)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.code = 'INTERN'
  AND p.code IN (
    'INTERN_READ'
    );
