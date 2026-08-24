-- V28__us006_email_verification_and_applications.sql
-- Migration for US-006: Candidate Email Verification, Email Logs, and Application Constraints

-- 1. Helper procedure to add column safely if missing
DROP PROCEDURE IF EXISTS AddColumnIfMissing;
DELIMITER //
CREATE PROCEDURE AddColumnIfMissing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_datatype VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_datatype);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- 2. Add email_verified to users table
CALL AddColumnIfMissing('users', 'email_verified', 'BOOLEAN NOT NULL DEFAULT FALSE');

-- 3. Create email_verification_tokens table
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    verified_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_email_verification_token_hash (token_hash),
    INDEX idx_email_verification_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 4. Create email_logs table
CREATE TABLE IF NOT EXISTS email_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient VARCHAR(255) NOT NULL,
    template_code VARCHAR(100) NOT NULL,
    reference_type VARCHAR(100) NULL,
    reference_id BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    error_message TEXT NULL,
    sent_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_logs_recipient (recipient),
    INDEX idx_email_logs_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Cleanup helper procedure
DROP PROCEDURE IF EXISTS AddColumnIfMissing;
