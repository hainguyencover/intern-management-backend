-- V22__align_state_machine_and_business_rules.sql
-- Align application and intern profile status constraints with Business Baseline State Machine

-- Helper procedure to drop any constraint safely
DROP PROCEDURE IF EXISTS DropAnyConstraint;
DELIMITER //
CREATE PROCEDURE DropAnyConstraint(
    IN p_table VARCHAR(64),
    IN p_constraint VARCHAR(64)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
    
    SET @sql1 = CONCAT('ALTER TABLE ', p_table, ' DROP CHECK ', p_constraint);
    PREPARE stmt1 FROM @sql1;
    EXECUTE stmt1;
    DEALLOCATE PREPARE stmt1;

    SET @sql2 = CONCAT('ALTER TABLE ', p_table, ' DROP CONSTRAINT ', p_constraint);
    PREPARE stmt2 FROM @sql2;
    EXECUTE stmt2;
    DEALLOCATE PREPARE stmt2;
END //
DELIMITER ;

-- 1. Applications check constraint alignment
CALL DropAnyConstraint('applications', 'ck_applications_status');

ALTER TABLE applications 
  ADD CONSTRAINT ck_applications_status 
  CHECK (status IN ('DRAFT', 'SUBMITTED', 'REVIEWING', 'SCREENING', 'INTERVIEWING', 'APPROVED', 'REJECTED', 'CONTRACT_SENT', 'CONTRACT_SIGNED', 'INTERNING', 'COMPLETED'));

-- 2. Update default status for intern_profiles to DRAFT
ALTER TABLE intern_profiles MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'DRAFT';

-- Update existing ONBOARDING statuses to INTERNING / DRAFT as appropriate
UPDATE intern_profiles SET status = 'DRAFT' WHERE status = 'ONBOARDING';

-- 3. Add performance index safely
DROP PROCEDURE IF EXISTS AddIndexIfMissing;
DELIMITER //
CREATE PROCEDURE AddIndexIfMissing(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = p_table
        AND INDEX_NAME = p_index
    ) THEN
        SET @sql = CONCAT('CREATE INDEX ', p_index, ' ON ', p_table, '(', p_columns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL AddIndexIfMissing('applications', 'idx_applications_intern_status', 'intern_id, status');

-- Cleanup helpers
DROP PROCEDURE IF EXISTS DropAnyConstraint;
DROP PROCEDURE IF EXISTS AddIndexIfMissing;
