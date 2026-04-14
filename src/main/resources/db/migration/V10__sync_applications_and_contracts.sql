/**
 * V10__sync_schema_defensive.sql
 * Robustly adds missing columns identifying during runtime debug.
 */

-- Helper Procedure to add columns idempotently
DROP PROCEDURE IF EXISTS AddColumnIfMissing;
DELIMITER //
CREATE PROCEDURE AddColumnIfMissing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = p_table
        AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- 1. Applications
CALL AddColumnIfMissing('applications', 'program_id', 'BIGINT NULL AFTER intern_id');
UPDATE applications SET program_id = (SELECT id FROM programs LIMIT 1) WHERE program_id IS NULL AND (SELECT COUNT(*) FROM programs) > 0;
-- We can't safely make it NOT NULL if there are no programs, but usually there are.
-- ALTER TABLE applications MODIFY COLUMN program_id BIGINT NOT NULL; 

-- 2. Internship Contracts
CALL AddColumnIfMissing('internship_contracts', 'file_url', 'VARCHAR(1000) NULL');
CALL AddColumnIfMissing('internship_contracts', 'status', 'VARCHAR(30) NOT NULL DEFAULT "SENT"');
CALL AddColumnIfMissing('internship_contracts', 'audit_created_by', 'VARCHAR(255) NULL');
CALL AddColumnIfMissing('internship_contracts', 'audit_updated_by', 'VARCHAR(255) NULL');


-- Helper for constraints
DROP PROCEDURE IF EXISTS AddConstraintIfMissing;
DELIMITER //
CREATE PROCEDURE AddConstraintIfMissing(
    IN p_table VARCHAR(64),
    IN p_constraint VARCHAR(64),
    IN p_definition VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
        AND TABLE_NAME = p_table
        AND CONSTRAINT_NAME = p_constraint
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD CONSTRAINT ', p_constraint, ' ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- Constraints
CALL AddConstraintIfMissing('applications', 'fk_applications_program', 'FOREIGN KEY (program_id) REFERENCES programs (id)');

-- Clean up helpers
DROP PROCEDURE IF EXISTS AddColumnIfMissing;
DROP PROCEDURE IF EXISTS AddConstraintIfMissing;
