-- V62__create_university_portal_schema.sql

-- 1. Create universities table
CREATE TABLE IF NOT EXISTS universities (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    email_domain VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Create university_users table (maps University to User account)
CREATE TABLE IF NOT EXISTS university_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    university_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_univ_user_university FOREIGN KEY (university_id) REFERENCES universities(id) ON DELETE CASCADE,
    CONSTRAINT fk_univ_user_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_univ_user_pair UNIQUE (university_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Insert default universities
INSERT IGNORE INTO universities (code, name, email_domain, status) VALUES
('FPT', 'FPT University', 'fpt.edu.vn', 'ACTIVE'),
('HUST', 'Hanoi University of Science and Technology', 'hust.edu.vn', 'ACTIVE'),
('PTIT', 'Posts and Telecommunications Institute of Technology', 'ptit.edu.vn', 'ACTIVE'),
('VNU', 'Vietnam National University', 'vnu.edu.vn', 'ACTIVE'),
('UIT', 'University of Information Technology', 'uit.edu.vn', 'ACTIVE'),
('DUT', 'Danang University of Technology', 'dut.udn.vn', 'ACTIVE');

-- 4. Add university_id to intern_profiles
SET @exist_col := (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'intern_profiles' 
      AND COLUMN_NAME = 'university_id'
);

SET @sql := IF(@exist_col = 0, 
    'ALTER TABLE intern_profiles ADD COLUMN university_id BIGINT NULL, ADD CONSTRAINT fk_intern_profiles_university FOREIGN KEY (university_id) REFERENCES universities(id) ON DELETE SET NULL', 
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. Backfill intern_profiles.university_id based on text column
UPDATE intern_profiles ip
JOIN universities u ON LOWER(ip.university) LIKE CONCAT('%', LOWER(u.code), '%') OR LOWER(ip.university) LIKE CONCAT('%', LOWER(u.name), '%')
SET ip.university_id = u.id
WHERE ip.university_id IS NULL;

-- Default remaining intern profiles to FPT University (id = 1) if not matched
UPDATE intern_profiles
SET university_id = (SELECT id FROM universities WHERE code = 'FPT' LIMIT 1)
WHERE university_id IS NULL;

-- 6. Insert RBAC roles for University
INSERT IGNORE INTO roles (code, name, description) VALUES
('UNIVERSITY_ADMIN', 'Quản trị viên trường đại học', 'Quyền xem toàn bộ tiến độ, báo cáo, chấm công của SV thuộc trường'),
('UNIVERSITY_VIEWER', 'Người xem cổng trường đại học', 'Quyền xem tiến độ sinh viên thuộc trường');

-- 7. Insert permissions for University Portal
INSERT IGNORE INTO permissions (code, name, description) VALUES
('UNIVERSITY_DASHBOARD_VIEW', 'Xem Dashboard trường đại học', 'Quyền xem tổng quan KPI thực tập của sinh viên trường'),
('UNIVERSITY_STUDENT_VIEW', 'Xem tiến độ sinh viên trường', 'Quyền xem danh sách & chi tiết sinh viên thuộc trường'),
('UNIVERSITY_ATTENDANCE_VIEW', 'Xem chấm công sinh viên trường', 'Quyền xem tổng hợp chuyên cần của sinh viên trường'),
('UNIVERSITY_EVALUATION_VIEW', 'Xem đánh giá sinh viên trường', 'Quyền xem kết quả đánh giá thực tập của sinh viên trường'),
('UNIVERSITY_REPORT_EXPORT', 'Xuất báo cáo trường đại học', 'Quyền xuất báo cáo tiến độ thực tập của trường');

-- 8. Map permissions to roles
-- ADMIN role gets all university permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code LIKE 'UNIVERSITY_%';

-- UNIVERSITY_ADMIN role gets all university permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'UNIVERSITY_ADMIN' AND p.code LIKE 'UNIVERSITY_%';

-- UNIVERSITY_VIEWER role gets view-only university permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('UNIVERSITY_DASHBOARD_VIEW', 'UNIVERSITY_STUDENT_VIEW', 'UNIVERSITY_ATTENDANCE_VIEW', 'UNIVERSITY_EVALUATION_VIEW')
WHERE r.code = 'UNIVERSITY_VIEWER';

-- 9. Add performance indexes for University reporting
CREATE INDEX idx_intern_university_status ON intern_profiles(university_id, status);
