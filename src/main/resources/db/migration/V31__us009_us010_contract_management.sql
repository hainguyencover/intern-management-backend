/**
 * V31__us009_us010_contract_management.sql
 * Schema setup for Contract Management (US-009 & US-010) enforcing BR-07 dual confirmation.
 */

-- Helper procedure to drop check constraints safely
DROP PROCEDURE IF EXISTS DropAnyConstraintContract;
DELIMITER //
CREATE PROCEDURE DropAnyConstraintContract(
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

CALL DropAnyConstraintContract('internship_contracts', 'ck_contracts_status');
CALL DropAnyConstraintContract('internship_contracts', 'ck_internship_contracts_status');
DROP PROCEDURE IF EXISTS DropAnyConstraintContract;

CREATE TABLE IF NOT EXISTS internship_contracts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    intern_id BIGINT NULL,
    file_name VARCHAR(255) NULL,
    file_url VARCHAR(1000) NULL,
    storage_key VARCHAR(1000) NULL,
    file_size BIGINT NULL DEFAULT 0,
    mime_type VARCHAR(100) NULL DEFAULT 'application/pdf',
    status VARCHAR(30) NOT NULL DEFAULT 'SENT',
    uploaded_by BIGINT NULL,
    uploaded_at DATETIME NULL,
    hr_confirmed_by BIGINT NULL,
    hr_confirmed_at DATETIME NULL,
    intern_confirmed_by BIGINT NULL,
    intern_confirmed_at DATETIME NULL,
    signed_at DATETIME NULL,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_id BIGINT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,
    CONSTRAINT uk_contracts_application_id UNIQUE (application_id)
);

-- Helper procedure to idempotently add columns if the table pre-existed
DROP PROCEDURE IF EXISTS AddColumnIfMissingContract;
DELIMITER //
CREATE PROCEDURE AddColumnIfMissingContract(
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

CALL AddColumnIfMissingContract('internship_contracts', 'intern_id', 'BIGINT NULL AFTER application_id');
CALL AddColumnIfMissingContract('internship_contracts', 'file_name', 'VARCHAR(255) NULL');
CALL AddColumnIfMissingContract('internship_contracts', 'storage_key', 'VARCHAR(1000) NULL');
CALL AddColumnIfMissingContract('internship_contracts', 'file_size', 'BIGINT NULL DEFAULT 0');
CALL AddColumnIfMissingContract('internship_contracts', 'mime_type', 'VARCHAR(100) NULL DEFAULT "application/pdf"');
CALL AddColumnIfMissingContract('internship_contracts', 'uploaded_by', 'BIGINT NULL');
CALL AddColumnIfMissingContract('internship_contracts', 'uploaded_at', 'DATETIME NULL');
CALL AddColumnIfMissingContract('internship_contracts', 'hr_confirmed_by', 'BIGINT NULL');
CALL AddColumnIfMissingContract('internship_contracts', 'hr_confirmed_at', 'DATETIME NULL');
CALL AddColumnIfMissingContract('internship_contracts', 'intern_confirmed_by', 'BIGINT NULL');
CALL AddColumnIfMissingContract('internship_contracts', 'intern_confirmed_at', 'DATETIME NULL');
CALL AddColumnIfMissingContract('internship_contracts', 'version', 'BIGINT NOT NULL DEFAULT 0');

DROP PROCEDURE IF EXISTS AddColumnIfMissingContract;
