-- ================================================================================
-- V27: Enhance Intern Documents Schema (US-004 & US-005)
-- Target: Add metadata fields (original_file_name, stored_file_name, storage_key, 
--         content_type, file_size, checksum, rejection_reason) and performance indexes.
-- ================================================================================

DROP PROCEDURE IF EXISTS AddColumnIfMissingV27;
DELIMITER //
CREATE PROCEDURE AddColumnIfMissingV27(
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

CALL AddColumnIfMissingV27('intern_documents', 'original_file_name', 'VARCHAR(255) NULL');
CALL AddColumnIfMissingV27('intern_documents', 'stored_file_name', 'VARCHAR(255) NULL');
CALL AddColumnIfMissingV27('intern_documents', 'storage_key', 'VARCHAR(500) NULL');
CALL AddColumnIfMissingV27('intern_documents', 'content_type', 'VARCHAR(100) NULL');
CALL AddColumnIfMissingV27('intern_documents', 'file_size', 'BIGINT NULL');
CALL AddColumnIfMissingV27('intern_documents', 'checksum', 'VARCHAR(64) NULL');
CALL AddColumnIfMissingV27('intern_documents', 'rejection_reason', 'VARCHAR(1000) NULL');

DROP PROCEDURE AddColumnIfMissingV27;

-- Add performance indexes if not existing
DROP PROCEDURE IF EXISTS AddIndexIfMissingV27;
DELIMITER //
CREATE PROCEDURE AddIndexIfMissingV27(
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
        SET @sql = CONCAT('CREATE INDEX ', p_index, ' ON ', p_table, ' (', p_columns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL AddIndexIfMissingV27('intern_documents', 'idx_document_intern_type', 'intern_id, type');
CALL AddIndexIfMissingV27('intern_documents', 'idx_document_status', 'status');
CALL AddIndexIfMissingV27('intern_documents', 'idx_document_tenant_status', 'tenant_id, status');

DROP PROCEDURE AddIndexIfMissingV27;
