-- V44__refactor_evaluations_for_state_machine.sql
-- US-019 Phase 2: Evaluation state machine, items table, and refactored constraints

-- ============================================================
-- Add new columns to evaluations table
-- ============================================================

-- Template reference
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'template_id') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN template_id BIGINT NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Program reference
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'program_id') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN program_id BIGINT NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Status (state machine)
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'status') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT ''LOCKED'''));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Overall score (backend-calculated)
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'overall_score') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN overall_score DECIMAL(5,2) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Overall comment (replaces simple comment for new flow)
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'overall_comment') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN overall_comment VARCHAR(4000) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Timestamp trail
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'submitted_at') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN submitted_at DATETIME NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'approved_at') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN approved_at DATETIME NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'locked_at') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN locked_at DATETIME NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'returned_at') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN returned_at DATETIME NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'return_reason') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN return_reason VARCHAR(2000) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Classification
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'classification') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN classification VARCHAR(30) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Optimistic locking
SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'version') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN version BIGINT DEFAULT 0'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ============================================================
-- evaluation_items: scores per criterion (snapshot-based)
-- ============================================================
CREATE TABLE IF NOT EXISTS evaluation_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id BIGINT NOT NULL,
    criterion_id BIGINT NOT NULL,
    score DECIMAL(4,2) NULL,
    comment VARCHAR(2000),
    weight_snapshot DECIMAL(5,2) NOT NULL,
    max_score_snapshot DECIMAL(4,2) NOT NULL DEFAULT 10.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_eval_item_evaluation FOREIGN KEY (evaluation_id)
        REFERENCES evaluations(id) ON DELETE CASCADE,
    CONSTRAINT fk_eval_item_criterion FOREIGN KEY (criterion_id)
        REFERENCES evaluation_criteria(id),
    UNIQUE INDEX uk_eval_item (evaluation_id, criterion_id),
    INDEX idx_eval_item_evaluation (evaluation_id)
);


-- ============================================================
-- Unique constraint: one evaluation per intern per period per tenant
-- ============================================================
-- First check if the index already exists
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'evaluations'
    AND INDEX_NAME = 'uk_eval_intern_period');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE evaluations ADD UNIQUE INDEX uk_eval_intern_period (tenant_id, intern_id, period)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ============================================================
-- Migrate existing evaluations: set status to LOCKED
-- (existing data was created without state machine)
-- ============================================================
UPDATE evaluations SET status = 'LOCKED'
WHERE status IS NULL OR status = '' OR status = 'DRAFT';

-- Copy weighted_score to overall_score if not set
UPDATE evaluations SET overall_score = weighted_score
WHERE overall_score IS NULL AND weighted_score IS NOT NULL;

-- Copy comment to overall_comment if not set
UPDATE evaluations SET overall_comment = comment
WHERE overall_comment IS NULL AND comment IS NOT NULL;
