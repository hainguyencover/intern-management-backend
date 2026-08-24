-- ================================================================================
-- V25: Ensure Default Department and Backfill Programs FK
-- Target: Fix Null Pointer and DataAccess exceptions for programs API
-- ================================================================================

-- 1. Ensure default department 1 exists
INSERT INTO departments (id, tenant_id, code, name, description)
VALUES (1, 1, 'ENG', 'Phòng Công nghệ & Phần mềm', 'Bộ phận kỹ thuật & phát triển phần mềm')
AS n ON DUPLICATE KEY UPDATE name = n.name;

-- 2. Update existing programs with NULL department_id
UPDATE programs SET department_id = 1 WHERE department_id IS NULL;
