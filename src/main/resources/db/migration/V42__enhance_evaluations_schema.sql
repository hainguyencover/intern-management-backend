-- V42__enhance_evaluations_schema.sql
-- US-019: Multi-criteria evaluation schema enhancement for Mentor Intern Evaluation

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'tenant_id') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'technical_score') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN technical_score DECIMAL(4,2) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'work_quality_score') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN work_quality_score DECIMAL(4,2) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'attitude_score') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN attitude_score DECIMAL(4,2) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'soft_skill_score') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN soft_skill_score DECIMAL(4,2) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'weighted_score') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN weighted_score DECIMAL(4,2) NULL'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'evaluations' AND COLUMN_NAME = 'result_status') > 0, 'SELECT 1', 'ALTER TABLE evaluations ADD COLUMN result_status VARCHAR(20) NULL DEFAULT ''PASS'''));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Indexes for fast querying
ALTER TABLE evaluations ADD INDEX IF NOT EXISTS idx_eval_intern (tenant_id, intern_id);
ALTER TABLE evaluations ADD INDEX IF NOT EXISTS idx_eval_mentor (tenant_id, mentor_id);
