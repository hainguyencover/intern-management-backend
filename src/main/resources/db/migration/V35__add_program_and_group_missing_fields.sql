/**
 * V35__add_program_and_group_missing_fields.sql
 * Schema updates for programs, program_groups, internship_enrollments, and mentor_assignments.
 */

-- 1. Helper procedure using valid MySQL stored procedure syntax
DROP PROCEDURE IF EXISTS AddProgramAndGroupColumnsV35;
DELIMITER //
CREATE PROCEDURE AddProgramAndGroupColumnsV35()
BEGIN
    -- programs.code
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'programs' AND COLUMN_NAME = 'code'
    ) THEN
        ALTER TABLE programs ADD COLUMN code VARCHAR(50) NULL;
    END IF;

    -- programs.max_interns
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'programs' AND COLUMN_NAME = 'max_interns'
    ) THEN
        ALTER TABLE programs ADD COLUMN max_interns INT NULL;
    END IF;

    -- program_groups.code
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'program_groups' AND COLUMN_NAME = 'code'
    ) THEN
        ALTER TABLE program_groups ADD COLUMN code VARCHAR(50) NULL;
    END IF;

    -- program_groups.description
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'program_groups' AND COLUMN_NAME = 'description'
    ) THEN
        ALTER TABLE program_groups ADD COLUMN description VARCHAR(500) NULL;
    END IF;

    -- program_groups.capacity
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'program_groups' AND COLUMN_NAME = 'capacity'
    ) THEN
        ALTER TABLE program_groups ADD COLUMN capacity INT NULL;
    END IF;

    -- program_groups.work_start_time
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'program_groups' AND COLUMN_NAME = 'work_start_time'
    ) THEN
        ALTER TABLE program_groups ADD COLUMN work_start_time TIME NULL;
    END IF;

    -- program_groups.work_end_time
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'program_groups' AND COLUMN_NAME = 'work_end_time'
    ) THEN
        ALTER TABLE program_groups ADD COLUMN work_end_time TIME NULL;
    END IF;

    -- program_groups.work_days
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'program_groups' AND COLUMN_NAME = 'work_days'
    ) THEN
        ALTER TABLE program_groups ADD COLUMN work_days VARCHAR(100) NULL;
    END IF;

END //
DELIMITER ;

CALL AddProgramAndGroupColumnsV35();
DROP PROCEDURE IF EXISTS AddProgramAndGroupColumnsV35;

-- 2. Backfill default values
UPDATE programs SET code = CONCAT('PROG-', id) WHERE code IS NULL OR code = '';
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
