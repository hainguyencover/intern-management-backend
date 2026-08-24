/**
 * V32__contract_lifecycle_and_audit_enhancements.sql
 * Schema setup for Contract Management Lifecycle & Signature Evidence Audit.
 */

-- Helper procedure to idempotently add columns to internship_contracts
DROP PROCEDURE IF EXISTS AddContractLifecycleColumns;
DELIMITER //
CREATE PROCEDURE AddContractLifecycleColumns()
BEGIN
    -- 1. revision_reason
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'internship_contracts' AND COLUMN_NAME = 'revision_reason'
    ) THEN
        ALTER TABLE internship_contracts ADD COLUMN revision_reason TEXT NULL;
    END IF;

    -- 2. revision_requested_at
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'internship_contracts' AND COLUMN_NAME = 'revision_requested_at'
    ) THEN
        ALTER TABLE internship_contracts ADD COLUMN revision_requested_at DATETIME NULL;
    END IF;

    -- 3. document_hash (SHA-256)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'internship_contracts' AND COLUMN_NAME = 'document_hash'
    ) THEN
        ALTER TABLE internship_contracts ADD COLUMN document_hash VARCHAR(64) NULL;
    END IF;

    -- 4. signed_ip
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'internship_contracts' AND COLUMN_NAME = 'signed_ip'
    ) THEN
        ALTER TABLE internship_contracts ADD COLUMN signed_ip VARCHAR(45) NULL;
    END IF;

    -- 5. signed_user_agent
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'internship_contracts' AND COLUMN_NAME = 'signed_user_agent'
    ) THEN
        ALTER TABLE internship_contracts ADD COLUMN signed_user_agent VARCHAR(500) NULL;
    END IF;
END //
DELIMITER ;

CALL AddContractLifecycleColumns();
DROP PROCEDURE IF EXISTS AddContractLifecycleColumns;
