/**
 * V11__sync_constraints_and_clean_up.sql
 * Fixes check constraints and renames columns to match entities across multiple tables.
 */

-- 0. Helper Procedure to add columns idempotently
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

-- 0.1 Helper Procedure to drop any type of constraint/check idempotently
DROP PROCEDURE IF EXISTS DropAnyConstraint;
DELIMITER //
CREATE PROCEDURE DropAnyConstraint(
    IN p_table VARCHAR(64),
    IN p_constraint VARCHAR(64)
)
BEGIN
    -- Try DROP CHECK (Common in MySQL 8.0 for CHECK constraints)
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
    
    SET @sql1 = CONCAT('ALTER TABLE ', p_table, ' DROP CHECK ', p_constraint);
    PREPARE stmt1 FROM @sql1;
    EXECUTE stmt1;
    DEALLOCATE PREPARE stmt1;

    -- Try DROP CONSTRAINT (Unified syntax in newer MySQL/MariaDB)
    SET @sql2 = CONCAT('ALTER TABLE ', p_table, ' DROP CONSTRAINT ', p_constraint);
    PREPARE stmt2 FROM @sql2;
    EXECUTE stmt2;
    DEALLOCATE PREPARE stmt2;
    
    -- Try DROP FOREIGN KEY (In case it was misidentified)
    SET @sql3 = CONCAT('ALTER TABLE ', p_table, ' DROP FOREIGN KEY ', p_constraint);
    PREPARE stmt3 FROM @sql3;
    EXECUTE stmt3;
    DEALLOCATE PREPARE stmt3;
END //
DELIMITER ;


-- 1. Fix 'intern_documents'
CALL DropAnyConstraint('intern_documents', 'ck_intern_documents_status');
CALL DropAnyConstraint('intern_documents', 'ck_intern_documents_type');
CALL DropAnyConstraint('intern_documents', 'ck_documents_status');
CALL DropAnyConstraint('intern_documents', 'ck_documents_type');
ALTER TABLE intern_documents MODIFY COLUMN status VARCHAR(30) NOT NULL;
ALTER TABLE intern_documents MODIFY COLUMN type VARCHAR(50) NOT NULL;

-- Add missing columns
CALL AddColumnIfMissing('intern_documents', 'uploaded_at', 'DATETIME NULL');

-- Rename note to review_note safely
DROP PROCEDURE IF EXISTS SyncDocuments;
DELIMITER //
CREATE PROCEDURE SyncDocuments()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_NAME='intern_documents' AND COLUMN_NAME='note') THEN
        ALTER TABLE intern_documents RENAME COLUMN note TO review_note;
    ELSE
        CALL AddColumnIfMissing('intern_documents', 'review_note', 'VARCHAR(1000) NULL');
    END IF;
END //
DELIMITER ;
CALL SyncDocuments();
DROP PROCEDURE SyncDocuments;


-- 2. Fix 'intern_profiles' (Add missing fields)
CALL AddColumnIfMissing('intern_profiles', 'phone_number', 'VARCHAR(20) NULL');
CALL AddColumnIfMissing('intern_profiles', 'address', 'VARCHAR(255) NULL');
CALL AddColumnIfMissing('intern_profiles', 'cv_url', 'VARCHAR(1000) NULL');
CALL AddColumnIfMissing('intern_profiles', 'skills', 'TEXT NULL');
CALL AddColumnIfMissing('intern_profiles', 'gender', 'VARCHAR(20) NULL');


-- 3. Fix 'attendances'
CALL AddColumnIfMissing('attendances', 'status', 'VARCHAR(20) NULL');


-- 4. Fix 'users' table
CALL AddColumnIfMissing('users', 'phone', 'VARCHAR(50) NULL');
CALL AddColumnIfMissing('users', 'address', 'VARCHAR(100) NULL');
CALL AddColumnIfMissing('users', 'status', 'VARCHAR(20) NOT NULL DEFAULT "ACTIVE"');
CALL AddColumnIfMissing('users', 'password_hash', 'VARCHAR(255) NULL');

-- Sync data and columns
DROP PROCEDURE IF EXISTS SyncUsers;
DELIMITER //
CREATE PROCEDURE SyncUsers()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_NAME='users' AND COLUMN_NAME='password') THEN
        UPDATE users SET password_hash = password WHERE password_hash IS NULL;
        ALTER TABLE users DROP COLUMN password;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_NAME='users' AND COLUMN_NAME='is_active') THEN
        UPDATE users SET status = 'ACTIVE' WHERE is_active = 1 AND (status IS NULL OR status = 'ACTIVE');
        UPDATE users SET status = 'INACTIVE' WHERE is_active = 0;
        ALTER TABLE users DROP COLUMN is_active;
    END IF;
END //
DELIMITER ;
CALL SyncUsers();
DROP PROCEDURE SyncUsers;


-- 5. Fix 'support_tickets'
CALL AddColumnIfMissing('support_tickets', 'priority', 'VARCHAR(20) NOT NULL DEFAULT "MEDIUM"');
CALL DropAnyConstraint('support_tickets', 'ck_support_tickets_category');
CALL DropAnyConstraint('support_tickets', 'ck_support_tickets_status');

-- 6. Fix 'applications'
CALL DropAnyConstraint('applications', 'ck_applications_status');


-- 7. Helper Cleanup
DROP PROCEDURE IF EXISTS AddColumnIfMissing;
DROP PROCEDURE IF EXISTS DropAnyConstraint;
