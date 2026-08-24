-- Flyway Migration V23: Seed 5 Test Records Per User Story Across All Roles (INTERN, MENTOR, HR, ADMIN)
-- Schema-compliant seeding aligned with Flyway V1-V22 table structures (MySQL 8.0+ syntax)

-- ============================================================================
-- 1. SEED USERS (HR, MENTOR, INTERN) & ROLE MAPPINGS
-- ============================================================================

INSERT INTO users (tenant_id, email, password_hash, full_name, status)
VALUES 
(1, 'hr1@company.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Nguyễn Thị HR 1', 'ACTIVE'),
(1, 'hr2@company.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Trần Văn HR 2', 'ACTIVE'),
(1, 'hr3@company.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Lê Thị HR 3', 'ACTIVE'),
(1, 'hr4@company.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Phạm Văn HR 4', 'ACTIVE'),
(1, 'hr5@company.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Hoàng Thị HR 5', 'ACTIVE')
AS n ON DUPLICATE KEY UPDATE full_name = n.full_name;

INSERT INTO users (tenant_id, email, password_hash, full_name, status)
VALUES 
(1, 'mentor1@company.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Nguyễn Văn Mentor 1', 'ACTIVE'),
(1, 'mentor2@company.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Trần Thị Mentor 2', 'ACTIVE'),
(1, 'mentor3@company.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Lê Văn Mentor 3', 'ACTIVE'),
(1, 'mentor4@company.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Phạm Thị Mentor 4', 'ACTIVE'),
(1, 'mentor5@company.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Vũ Văn Mentor 5', 'ACTIVE')
AS n ON DUPLICATE KEY UPDATE full_name = n.full_name;

INSERT INTO users (tenant_id, email, password_hash, full_name, status)
VALUES 
(1, 'intern1@student.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Nguyễn Văn Intern 1', 'ACTIVE'),
(1, 'intern2@student.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Trần Thị Intern 2', 'ACTIVE'),
(1, 'intern3@student.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Lê Văn Intern 3', 'ACTIVE'),
(1, 'intern4@student.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Phạm Thị Intern 4', 'ACTIVE'),
(1, 'intern5@student.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2', 'Đỗ Văn Intern 5', 'ACTIVE')
AS n ON DUPLICATE KEY UPDATE full_name = n.full_name;

-- Map user_roles without duplicate key warnings
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = 'HR' WHERE u.email LIKE 'hr%@company.com'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = 'MENTOR' WHERE u.email LIKE 'mentor%@company.com'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = 'INTERN' WHERE u.email LIKE 'intern%@student.com'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);

-- ============================================================================
-- 2. SEED MENTORS & INTERN PROFILES
-- ============================================================================

INSERT INTO mentors (user_id, title)
SELECT u.id, 'Senior Technical Mentor' FROM users u WHERE u.email = 'mentor1@company.com'
AS n ON DUPLICATE KEY UPDATE title = n.title;

INSERT INTO mentors (user_id, title)
SELECT u.id, 'Backend Technical Mentor' FROM users u WHERE u.email = 'mentor2@company.com'
AS n ON DUPLICATE KEY UPDATE title = n.title;

INSERT INTO intern_profiles (user_id, student_code, university, major, phone, address, gpa, status, start_date, end_date)
SELECT u.id, 'TTS2026-001', 'Đại học Bách Khoa Hà Nội', 'Khoa học Máy tính', '0901234561', 'Hà Nội', 3.6, 'INTERNING', '2026-06-01', '2026-08-31'
FROM users u WHERE u.email = 'intern1@student.com'
AS n ON DUPLICATE KEY UPDATE university = n.university;

INSERT INTO intern_profiles (user_id, student_code, university, major, phone, address, gpa, status, start_date, end_date)
SELECT u.id, 'TTS2026-002', 'Đại học Công nghệ - ĐHQGHN', 'Công nghệ Thông tin', '0901234562', 'Hà Nội', 3.8, 'INTERNING', '2026-06-01', '2026-08-31'
FROM users u WHERE u.email = 'intern2@student.com'
AS n ON DUPLICATE KEY UPDATE university = n.university;

INSERT INTO intern_profiles (user_id, student_code, university, major, phone, address, gpa, status, start_date, end_date)
SELECT u.id, 'TTS2026-003', 'Học viện Bưu chính Viễn thông', 'An toàn Thông tin', '0901234563', 'Hà Nội', 3.2, 'DRAFT', '2026-06-01', '2026-08-31'
FROM users u WHERE u.email = 'intern3@student.com'
AS n ON DUPLICATE KEY UPDATE university = n.university;

INSERT INTO intern_profiles (user_id, student_code, university, major, phone, address, gpa, status, start_date, end_date)
SELECT u.id, 'TTS2026-004', 'Đại học Bách Khoa Hà Nội', 'Hệ thống Thông tin', '0901234564', 'Hà Nội', 3.4, 'INTERNING', '2026-06-01', '2026-08-31'
FROM users u WHERE u.email = 'intern4@student.com'
AS n ON DUPLICATE KEY UPDATE university = n.university;

INSERT INTO intern_profiles (user_id, student_code, university, major, phone, address, gpa, status, start_date, end_date)
SELECT u.id, 'TTS2026-005', 'Đại học Bách Khoa Hà Nội', 'Kỹ thuật Phần mềm', '0901234565', 'Hà Nội', 2.9, 'COMPLETED', '2025-09-01', '2025-12-01'
FROM users u WHERE u.email = 'intern5@student.com'
AS n ON DUPLICATE KEY UPDATE university = n.university;

-- ============================================================================
-- 3. SEED DEPARTMENTS & PROGRAMS
-- ============================================================================

INSERT INTO departments (id, tenant_id, code, name, description)
VALUES (1, 1, 'ENG', 'Phòng Công nghệ & Phần mềm', 'Bộ phận kỹ thuật & phát triển phần mềm')
AS n ON DUPLICATE KEY UPDATE name = n.name;

INSERT INTO programs (tenant_id, department_id, name, description, start_date, end_date, status)
VALUES 
(1, 1, 'Software Engineer Intern 2026 - Batch 1', 'Chương trình tuyển dụng TTS Lập trình Backend Java & Vue.js', '2026-06-01', '2026-08-31', 'ACTIVE'),
(1, 1, 'Frontend Web Developer Intern 2026 - Batch 1', 'Chương trình tuyển dụng TTS Lập trình Vue.js 3 & TypeScript', '2026-06-01', '2026-08-31', 'ACTIVE'),
(1, 1, 'QA & Automation Testing Intern 2026', 'Chương trình kiểm thử phần mềm tự động & Manual Test', '2026-06-15', '2026-09-15', 'ACTIVE'),
(1, 1, 'Mobile App React Native Intern 2026', 'Chương trình phát triển ứng dụng di động đa nền tảng', '2026-07-01', '2026-09-30', 'ACTIVE'),
(1, 1, 'DevOps & Cloud Systems Intern 2026', 'Chương trình thực tập hạ tầng Cloud, Docker & CI/CD', '2026-07-15', '2026-10-15', 'CLOSED')
AS n ON DUPLICATE KEY UPDATE name = n.name;

-- ============================================================================
-- 4. SEED APPLICATIONS
-- ============================================================================

INSERT INTO applications (tenant_id, intern_id, program_id, position, status, note, ai_score)
SELECT 1, ip.id, p.id, 'Java Backend Developer', 'APPROVED', 'Hồ sơ đạt yêu cầu AI Screening 88%', 88
FROM intern_profiles ip JOIN programs p ON p.name LIKE 'Software Engineer%' JOIN users u ON ip.user_id = u.id WHERE u.email = 'intern1@student.com'
AS n ON DUPLICATE KEY UPDATE status = n.status;

INSERT INTO applications (tenant_id, intern_id, program_id, position, status, note, ai_score)
SELECT 1, ip.id, p.id, 'Vue.js Frontend Developer', 'APPROVED', 'Phù hợp kiến thức Frontend Vue 3', 82
FROM intern_profiles ip JOIN programs p ON p.name LIKE 'Frontend Web Developer%' JOIN users u ON ip.user_id = u.id WHERE u.email = 'intern2@student.com'
AS n ON DUPLICATE KEY UPDATE status = n.status;

-- ============================================================================
-- 5. SEED CONTRACTS
-- ============================================================================

INSERT INTO internship_contracts (application_id, file_url, signed_at, status)
SELECT a.id, '/uploads/contracts/CTR-001.pdf', '2026-06-02 10:00:00', 'SIGNED'
FROM applications a LIMIT 1
AS n ON DUPLICATE KEY UPDATE status = n.status;

-- ============================================================================
-- 6. SEED PROGRAM GROUPS & TASKS
-- ============================================================================

INSERT INTO program_groups (program_id, name, status)
SELECT p.id, 'Group Java Backend 01', 'ACTIVE' FROM programs p LIMIT 1
AS n ON DUPLICATE KEY UPDATE name = n.name;

INSERT INTO tasks (group_id, title, description, due_date, status)
SELECT pg.id, 'Thiết kế Database ERD & Flyway Migration', 'Xây dựng bảng dữ liệu MySQL 8.0 cho module Attendance', '2026-08-10 18:00:00', 'DONE'
FROM program_groups pg LIMIT 1
AS n ON DUPLICATE KEY UPDATE title = n.title;

-- ============================================================================
-- 7. SEED WEEKLY REPORTS
-- ============================================================================

INSERT INTO weekly_reports (intern_id, week_number, title, week_start, week_end, report_date, completed_work, status, mentor_feedback)
SELECT ip.id, 1, 'Báo cáo tuần 1: Nắm bắt hệ thống & Database Schema', '2026-06-01', '2026-06-07', '2026-06-07', 'Hoàn thành tìm hiểu Spring Boot & Vue.js', 'REVIEWED', 'Đã duyệt, hoàn thành tốt nhiệm vụ'
FROM intern_profiles ip JOIN users u ON ip.user_id = u.id WHERE u.email = 'intern1@student.com'
AS n ON DUPLICATE KEY UPDATE title = n.title;

-- ============================================================================
-- 8. SEED EVALUATIONS
-- ============================================================================

INSERT INTO evaluations (intern_id, mentor_id, period, score, comment)
SELECT ip.id, m.id, 'FINAL', 9, 'Thực tập sinh đạt kết quả xuất sắc'
FROM intern_profiles ip JOIN users u1 ON ip.user_id = u1.id, mentors m JOIN users u2 ON m.user_id = u2.id
WHERE u1.email = 'intern1@student.com' AND u2.email = 'mentor1@company.com' LIMIT 1
AS n ON DUPLICATE KEY UPDATE comment = n.comment;

-- ============================================================================
-- 9. SEED LEAVE REQUESTS
-- ============================================================================

INSERT INTO leave_requests (intern_id, start_date, end_date, reason, status)
SELECT ip.id, '2026-07-01', '2026-07-02', 'Xin nghỉ phép bảo vệ đồ án tốt nghiệp tại Trường Đại học', 'APPROVED'
FROM intern_profiles ip JOIN users u ON ip.user_id = u.id WHERE u.email = 'intern1@student.com'
AS n ON DUPLICATE KEY UPDATE reason = n.reason;

-- ============================================================================
-- 10. SEED SUPPORT TICKETS
-- ============================================================================

INSERT INTO support_tickets (created_by, category, title, content, status)
SELECT u.id, 'CERTIFICATE', 'Yêu cầu xin Giấy xác nhận thực tập gửi Trường', 'Kính gửi HR, em cần xin giấy xác nhận thực tập để nộp về khoa CNTT Bách Khoa.', 'RESOLVED'
FROM users u WHERE u.email = 'intern1@student.com'
AS n ON DUPLICATE KEY UPDATE title = n.title;
