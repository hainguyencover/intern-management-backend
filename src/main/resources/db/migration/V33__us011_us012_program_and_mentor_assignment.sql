/**
 * V33__us011_us012_program_and_mentor_assignment.sql
 * Enhancements for Internship Programs, Groups, Enrollments, and Mentor Assignments.
 */

-- 1. Upgrade programs table
ALTER TABLE programs ADD COLUMN IF NOT EXISTS code VARCHAR(50) NULL AFTER department_id;
ALTER TABLE programs ADD COLUMN IF NOT EXISTS max_interns INT NULL AFTER status;
UPDATE programs SET code = CONCAT('PROG-', id) WHERE code IS NULL OR code = '';

-- 2. Upgrade program_groups table
ALTER TABLE program_groups ADD COLUMN IF NOT EXISTS code VARCHAR(50) NULL AFTER name;
ALTER TABLE program_groups ADD COLUMN IF NOT EXISTS description VARCHAR(500) NULL AFTER code;
ALTER TABLE program_groups ADD COLUMN IF NOT EXISTS capacity INT NULL AFTER status;
ALTER TABLE program_groups ADD COLUMN IF NOT EXISTS work_start_time TIME NULL;
ALTER TABLE program_groups ADD COLUMN IF NOT EXISTS work_end_time TIME NULL;
ALTER TABLE program_groups ADD COLUMN IF NOT EXISTS work_days VARCHAR(100) NULL;
UPDATE program_groups SET code = CONCAT('GRP-', id) WHERE code IS NULL OR code = '';

-- 3. Create internship_enrollments table
CREATE TABLE IF NOT EXISTS internship_enrollments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    program_id BIGINT NOT NULL,
    group_id BIGINT NULL,
    intern_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATE NOT NULL,
    ended_at DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_enrollment_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_enrollment_program FOREIGN KEY (program_id) REFERENCES programs(id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_group FOREIGN KEY (group_id) REFERENCES program_groups(id) ON DELETE SET NULL,
    CONSTRAINT fk_enrollment_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id) ON DELETE CASCADE,
    INDEX idx_enrollment_intern (intern_id),
    INDEX idx_enrollment_program (program_id),
    INDEX idx_enrollment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Create mentor_assignments table
CREATE TABLE IF NOT EXISTS mentor_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    enrollment_id BIGINT NULL,
    intern_id BIGINT NOT NULL,
    mentor_id BIGINT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    assigned_by BIGINT NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mentor_assign_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_mentor_assign_enrollment FOREIGN KEY (enrollment_id) REFERENCES internship_enrollments(id) ON DELETE SET NULL,
    CONSTRAINT fk_mentor_assign_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_mentor_assign_mentor FOREIGN KEY (mentor_id) REFERENCES mentors(id) ON DELETE CASCADE,
    CONSTRAINT fk_mentor_assign_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_assignment_mentor (mentor_id),
    INDEX idx_assignment_intern (intern_id),
    INDEX idx_assignment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
