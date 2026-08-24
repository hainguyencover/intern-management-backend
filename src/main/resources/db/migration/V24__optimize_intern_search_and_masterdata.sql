-- ================================================================================
-- V24: Optimize Intern Search, Master Data Tables, and Optimistic Locking Version
-- Target: US-001, US-002, US-003 Vertical Slice
-- ================================================================================

-- 1. Add version column to intern_profiles for JPA Optimistic Locking (@Version)
ALTER TABLE intern_profiles ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 2. Master Data: schools
CREATE TABLE IF NOT EXISTS schools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_created_by VARCHAR(255),
    audit_updated_by VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Master Data: majors
CREATE TABLE IF NOT EXISTS majors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_created_by VARCHAR(255),
    audit_updated_by VARCHAR(255),
    CONSTRAINT fk_majors_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE,
    UNIQUE KEY uk_majors_school_code (school_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default schools
INSERT INTO schools (id, name, code, tenant_id) VALUES
(1, 'Đại học Bách Khoa Hà Nội', 'HUST', 1),
(2, 'Đại học Công nghệ - ĐHQGHN', 'UET', 1),
(3, 'Học viện Bưu chính Viễn thông', 'PTIT', 1),
(4, 'Đại học FPT', 'FPT', 1)
AS n ON DUPLICATE KEY UPDATE name = n.name;

-- Seed default majors
INSERT INTO majors (id, school_id, name, code, tenant_id) VALUES
(1, 1, 'Khoa học Máy tính', 'CS', 1),
(2, 1, 'Kỹ thuật Máy tính', 'CE', 1),
(3, 2, 'Công nghệ Thông tin', 'IT', 1),
(4, 2, 'Khoa học Dữ liệu', 'DS', 1),
(5, 3, 'An toàn Thông tin', 'IS', 1),
(6, 4, 'Kỹ thuật Phần mềm', 'SE', 1)
AS n ON DUPLICATE KEY UPDATE name = n.name;
