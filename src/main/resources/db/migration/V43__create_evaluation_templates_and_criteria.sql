-- V43__create_evaluation_templates_and_criteria.sql
-- US-019 Phase 1: Evaluation Template & Criteria Engine
-- Enables dynamic, configurable evaluation rubrics per program

-- ============================================================
-- evaluation_templates: each program can have its own rubric
-- ============================================================
CREATE TABLE IF NOT EXISTS evaluation_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    program_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    evaluation_period VARCHAR(20) NOT NULL DEFAULT 'FINAL',
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    audit_created_by VARCHAR(255),
    audit_updated_by VARCHAR(255),
    INDEX idx_eval_template_tenant (tenant_id),
    INDEX idx_eval_template_program (tenant_id, program_id),
    INDEX idx_eval_template_status (tenant_id, status)
);

-- FK to programs (conditional — only if programs table exists)
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'evaluation_templates'
    AND CONSTRAINT_NAME = 'fk_eval_template_program');
SET @sql = IF(@fk_exists = 0,
    'ALTER TABLE evaluation_templates ADD CONSTRAINT fk_eval_template_program FOREIGN KEY (program_id) REFERENCES programs(id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ============================================================
-- evaluation_criteria: criteria belonging to a template
-- ============================================================
CREATE TABLE IF NOT EXISTS evaluation_criteria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    weight DECIMAL(5,2) NOT NULL,
    max_score DECIMAL(4,2) NOT NULL DEFAULT 10.00,
    display_order INT NOT NULL DEFAULT 0,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eval_criteria_template FOREIGN KEY (template_id)
        REFERENCES evaluation_templates(id) ON DELETE CASCADE,
    INDEX idx_eval_criteria_template (template_id),
    INDEX idx_eval_criteria_category (template_id, category)
);


-- ============================================================
-- Seed default template (used as fallback for all programs)
-- ============================================================
INSERT INTO evaluation_templates (tenant_id, name, description, evaluation_period, version, status)
VALUES (1, 'Default Internship Evaluation', 'Bộ đánh giá mặc định cho chương trình thực tập', 'FINAL', 1, 'ACTIVE');

SET @tmpl_id = LAST_INSERT_ID();

-- 10 criteria across 3 categories, total weight = 100%
INSERT INTO evaluation_criteria (template_id, category, name, description, weight, max_score, display_order, required) VALUES
(@tmpl_id, 'TECHNICAL',  'Technical Knowledge',          'Kiến thức kỹ thuật chuyên môn',                    15.00, 10.00, 1,  TRUE),
(@tmpl_id, 'TECHNICAL',  'Problem Solving',              'Khả năng phân tích và giải quyết vấn đề',          10.00, 10.00, 2,  TRUE),
(@tmpl_id, 'TECHNICAL',  'Code Quality / Work Quality',  'Chất lượng code hoặc sản phẩm công việc',          10.00, 10.00, 3,  TRUE),
(@tmpl_id, 'TECHNICAL',  'Learning Ability',             'Khả năng tiếp thu kiến thức mới và tự học',        10.00, 10.00, 4,  TRUE),
(@tmpl_id, 'SOFT_SKILL', 'Communication',                'Kỹ năng giao tiếp, trình bày và trao đổi',        10.00, 10.00, 5,  TRUE),
(@tmpl_id, 'SOFT_SKILL', 'Teamwork',                     'Khả năng phối hợp và làm việc nhóm',              10.00, 10.00, 6,  TRUE),
(@tmpl_id, 'SOFT_SKILL', 'Time Management',              'Quản lý thời gian, đúng deadline',                 10.00, 10.00, 7,  TRUE),
(@tmpl_id, 'ATTITUDE',   'Responsibility',               'Tinh thần trách nhiệm với công việc',              10.00, 10.00, 8,  TRUE),
(@tmpl_id, 'ATTITUDE',   'Discipline',                   'Kỷ luật, tuân thủ quy trình và nội quy',          10.00, 10.00, 9,  TRUE),
(@tmpl_id, 'ATTITUDE',   'Proactiveness',                'Tính chủ động, sáng tạo trong công việc',           5.00, 10.00, 10, TRUE);
