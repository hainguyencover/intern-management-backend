/**
 * V47__upgrade_support_ticket_schema.sql
 * Enhances Support Ticket schema for US-027 and US-028 (SLA tracking, assignment, attachments, status history, priority).
 */

-- Helper procedure to safely add columns if missing
DROP PROCEDURE IF EXISTS AddColumnIfMissingSupportTicket;
DELIMITER //
CREATE PROCEDURE AddColumnIfMissingSupportTicket(
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

-- Add missing columns to support_tickets
CALL AddColumnIfMissingSupportTicket('support_tickets', 'ticket_code', 'VARCHAR(50) NULL AFTER id');
CALL AddColumnIfMissingSupportTicket('support_tickets', 'priority', 'VARCHAR(20) NOT NULL DEFAULT "MEDIUM" AFTER category');
CALL AddColumnIfMissingSupportTicket('support_tickets', 'assigned_to', 'BIGINT NULL AFTER created_by');
CALL AddColumnIfMissingSupportTicket('support_tickets', 'resolution', 'TEXT NULL AFTER content');
CALL AddColumnIfMissingSupportTicket('support_tickets', 'first_response_at', 'DATETIME NULL AFTER updated_at');
CALL AddColumnIfMissingSupportTicket('support_tickets', 'first_response_due_at', 'DATETIME NULL AFTER first_response_at');
CALL AddColumnIfMissingSupportTicket('support_tickets', 'resolution_due_at', 'DATETIME NULL AFTER first_response_due_at');
CALL AddColumnIfMissingSupportTicket('support_tickets', 'resolved_at', 'DATETIME NULL AFTER resolution_due_at');
CALL AddColumnIfMissingSupportTicket('support_tickets', 'closed_at', 'DATETIME NULL AFTER resolved_at');
CALL AddColumnIfMissingSupportTicket('support_tickets', 'closed_by', 'BIGINT NULL AFTER closed_at');
CALL AddColumnIfMissingSupportTicket('support_tickets', 'version', 'BIGINT NOT NULL DEFAULT 0 AFTER closed_by');

DROP PROCEDURE IF EXISTS AddColumnIfMissingSupportTicket;

-- Backfill ticket_code and SLA dates for existing tickets
UPDATE support_tickets 
SET ticket_code = CONCAT('SUP-2026-', LPAD(id, 6, '0')) 
WHERE ticket_code IS NULL;

UPDATE support_tickets 
SET first_response_due_at = DATE_ADD(created_at, INTERVAL 3 DAY) 
WHERE first_response_due_at IS NULL AND created_at IS NOT NULL;

-- Make ticket_code NOT NULL and UNIQUE if unique index doesn't exist
DROP PROCEDURE IF EXISTS EnsureTicketCodeUnique;
DELIMITER //
CREATE PROCEDURE EnsureTicketCodeUnique()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'support_tickets'
        AND INDEX_NAME = 'uk_support_tickets_code'
    ) THEN
        ALTER TABLE support_tickets MODIFY COLUMN ticket_code VARCHAR(50) NOT NULL;
        ALTER TABLE support_tickets ADD CONSTRAINT uk_support_tickets_code UNIQUE (ticket_code);
    END IF;
END //
DELIMITER ;
CALL EnsureTicketCodeUnique();
DROP PROCEDURE IF EXISTS EnsureTicketCodeUnique;

-- Foreign key for assigned_to and closed_by if not existing
DROP PROCEDURE IF EXISTS AddSupportTicketFKs;
DELIMITER //
CREATE PROCEDURE AddSupportTicketFKs()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'support_tickets'
        AND CONSTRAINT_NAME = 'fk_support_tickets_assigned_to'
    ) THEN
        ALTER TABLE support_tickets ADD CONSTRAINT fk_support_tickets_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'support_tickets'
        AND CONSTRAINT_NAME = 'fk_support_tickets_closed_by'
    ) THEN
        ALTER TABLE support_tickets ADD CONSTRAINT fk_support_tickets_closed_by FOREIGN KEY (closed_by) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END //
DELIMITER ;
CALL AddSupportTicketFKs();
DROP PROCEDURE IF EXISTS AddSupportTicketFKs;

-- Add is_internal column to ticket_comments if missing
DROP PROCEDURE IF EXISTS AddIsInternalToComments;
DELIMITER //
CREATE PROCEDURE AddIsInternalToComments()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'ticket_comments'
        AND COLUMN_NAME = 'is_internal'
    ) THEN
        ALTER TABLE ticket_comments ADD COLUMN is_internal BOOLEAN NOT NULL DEFAULT FALSE AFTER content;
    END IF;
END //
DELIMITER ;
CALL AddIsInternalToComments();
DROP PROCEDURE IF EXISTS AddIsInternalToComments;

-- Create support_ticket_attachments table
CREATE TABLE IF NOT EXISTS support_ticket_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    comment_id BIGINT NULL,
    uploaded_by BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    checksum VARCHAR(100) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    deleted_at DATETIME NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,
    CONSTRAINT fk_ticket_attachments_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_attachments_comment FOREIGN KEY (comment_id) REFERENCES ticket_comments(id) ON DELETE SET NULL,
    CONSTRAINT fk_ticket_attachments_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE RESTRICT
);

-- Create support_ticket_status_history table
CREATE TABLE IF NOT EXISTS support_ticket_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    changed_by BIGINT NOT NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,
    CONSTRAINT fk_ticket_history_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_history_changed_by FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE RESTRICT
);

-- Indexes for performance & SLA monitoring
CREATE INDEX idx_support_tickets_tenant_status ON support_tickets (tenant_id, status);
CREATE INDEX idx_support_tickets_assigned ON support_tickets (assigned_to, status);
CREATE INDEX idx_support_tickets_intern ON support_tickets (created_by, created_at);
CREATE INDEX idx_support_tickets_sla ON support_tickets (first_response_due_at, status);
