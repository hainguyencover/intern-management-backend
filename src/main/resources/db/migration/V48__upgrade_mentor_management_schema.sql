-- V48__upgrade_mentor_management_schema.sql

-- 1. Upgrade mentors table
ALTER TABLE mentors
    ADD COLUMN IF NOT EXISTS employee_code VARCHAR(50) NULL AFTER user_id,
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(150) NULL AFTER employee_code,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30) NULL AFTER full_name,
    ADD COLUMN IF NOT EXISTS position VARCHAR(100) NULL AFTER department_id,
    ADD COLUMN IF NOT EXISTS specialization VARCHAR(255) NULL AFTER position,
    ADD COLUMN IF NOT EXISTS years_of_experience DECIMAL(5,2) NULL AFTER specialization,
    ADD COLUMN IF NOT EXISTS capacity INT NOT NULL DEFAULT 5 AFTER years_of_experience,
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' AFTER capacity,
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500) NULL AFTER status,
    ADD COLUMN IF NOT EXISTS bio TEXT NULL AFTER avatar_url,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0 AFTER bio;

-- Backfill mentors data from users table if full_name / phone / employee_code are null
UPDATE mentors m
JOIN users u ON m.user_id = u.id
SET m.full_name = COALESCE(m.full_name, u.full_name),
    m.phone = COALESCE(m.phone, u.phone),
    m.employee_code = COALESCE(m.employee_code, CONCAT('MTR-', LPAD(m.id, 5, '0')))
WHERE m.employee_code IS NULL OR m.full_name IS NULL;

-- Make employee_code & full_name NOT NULL after backfill
ALTER TABLE mentors MODIFY COLUMN employee_code VARCHAR(50) NOT NULL;
ALTER TABLE mentors MODIFY COLUMN full_name VARCHAR(150) NOT NULL;

-- Add unique constraint for employee_code per tenant
ALTER TABLE mentors ADD CONSTRAINT uk_mentor_employee_code UNIQUE (tenant_id, employee_code);

-- Add performance indexes for mentors
CREATE INDEX idx_mentor_tenant_status ON mentors (tenant_id, status);
CREATE INDEX idx_mentor_department ON mentors (tenant_id, department_id);

-- 2. Upgrade mentor_assignments table
ALTER TABLE mentor_assignments
    ADD COLUMN IF NOT EXISTS assignment_type VARCHAR(30) NOT NULL DEFAULT 'PRIMARY' AFTER mentor_id,
    ADD COLUMN IF NOT EXISTS start_date DATE NULL AFTER assignment_type,
    ADD COLUMN IF NOT EXISTS end_date DATE NULL AFTER start_date,
    ADD COLUMN IF NOT EXISTS responsibility TEXT NULL AFTER status,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0 AFTER responsibility;

-- Backfill start_date from assigned_at if start_date is NULL
UPDATE mentor_assignments
SET start_date = DATE(assigned_at)
WHERE start_date IS NULL;

ALTER TABLE mentor_assignments MODIFY COLUMN start_date DATE NOT NULL;

-- Add performance indexes for mentor_assignments
CREATE INDEX idx_assignment_tenant_mentor ON mentor_assignments (tenant_id, mentor_id);
CREATE INDEX idx_assignment_tenant_intern ON mentor_assignments (tenant_id, intern_id);
CREATE INDEX idx_assignment_tenant_status ON mentor_assignments (tenant_id, status);
CREATE INDEX idx_assignment_tenant_dates ON mentor_assignments (tenant_id, start_date, end_date);
