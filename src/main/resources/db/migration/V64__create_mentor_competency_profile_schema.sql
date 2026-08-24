-- V64__create_mentor_competency_profile_schema.sql

-- 1. Upgrade mentors table
ALTER TABLE mentors
    ADD COLUMN IF NOT EXISTS mentoring_experience_years INT NOT NULL DEFAULT 0 AFTER years_of_experience,
    ADD COLUMN IF NOT EXISTS profile_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' AFTER status;

-- 2. Create skills master table
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(100) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_skill_tenant_name (tenant_id, name),
    INDEX idx_skill_name (name),
    INDEX idx_skill_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 3. Create mentor_skills join table
CREATE TABLE IF NOT EXISTS mentor_skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mentor_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency_level VARCHAR(30) NOT NULL DEFAULT 'INTERMEDIATE',
    years_of_experience INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mentor_skill (mentor_id, skill_id),
    CONSTRAINT fk_mentor_skills_mentor FOREIGN KEY (mentor_id) REFERENCES mentors(id) ON DELETE CASCADE,
    CONSTRAINT fk_mentor_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 4. Create mentoring_domains master table
CREATE TABLE IF NOT EXISTS mentoring_domains (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_domain_tenant_name (tenant_id, name),
    INDEX idx_domain_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 5. Create mentor_domains join table
CREATE TABLE IF NOT EXISTS mentor_domains (
    mentor_id BIGINT NOT NULL,
    domain_id BIGINT NOT NULL,
    PRIMARY KEY (mentor_id, domain_id),
    CONSTRAINT fk_mentor_domains_mentor FOREIGN KEY (mentor_id) REFERENCES mentors(id) ON DELETE CASCADE,
    CONSTRAINT fk_mentor_domains_domain FOREIGN KEY (domain_id) REFERENCES mentoring_domains(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 6. Create mentor_experiences table
CREATE TABLE IF NOT EXISTS mentor_experiences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mentor_id BIGINT NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    position VARCHAR(150) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    description TEXT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mentor_experiences_mentor FOREIGN KEY (mentor_id) REFERENCES mentors(id) ON DELETE CASCADE,
    INDEX idx_mentor_experience_mentor (mentor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 7. Create mentor_certifications table
CREATE TABLE IF NOT EXISTS mentor_certifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mentor_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    issuing_organization VARCHAR(200) NULL,
    credential_id VARCHAR(150) NULL,
    issued_date DATE NULL,
    expiry_date DATE NULL,
    credential_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mentor_certifications_mentor FOREIGN KEY (mentor_id) REFERENCES mentors(id) ON DELETE CASCADE,
    INDEX idx_mentor_cert_mentor (mentor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 8. Create mentor_profile_audits table
CREATE TABLE IF NOT EXISTS mentor_profile_audits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mentor_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    changed_by BIGINT NOT NULL,
    old_value JSON NULL,
    new_value JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mentor_audits_mentor FOREIGN KEY (mentor_id) REFERENCES mentors(id) ON DELETE CASCADE,
    INDEX idx_mentor_audit (mentor_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 9. Seed default master data for global skills (tenant_id IS NULL)
INSERT IGNORE INTO skills (tenant_id, name, category) VALUES
(NULL, 'Java', 'Backend'),
(NULL, 'Spring Boot', 'Backend'),
(NULL, 'Node.js', 'Backend'),
(NULL, 'Python', 'Backend'),
(NULL, 'Vue.js', 'Frontend'),
(NULL, 'React', 'Frontend'),
(NULL, 'TypeScript', 'Frontend'),
(NULL, 'SQL / MySQL', 'Database'),
(NULL, 'PostgreSQL', 'Database'),
(NULL, 'Docker', 'DevOps'),
(NULL, 'Kubernetes', 'DevOps'),
(NULL, 'CI/CD', 'DevOps'),
(NULL, 'Automation Testing', 'QA'),
(NULL, 'Manual Testing', 'QA'),
(NULL, 'UI/UX Design', 'Design'),
(NULL, 'Data Engineering', 'Data');

-- 10. Seed default master data for global mentoring domains (tenant_id IS NULL)
INSERT IGNORE INTO mentoring_domains (tenant_id, name, description) VALUES
(NULL, 'Backend Development', 'Phát triển hệ thống Backend, API, Microservices'),
(NULL, 'Frontend Development', 'Phát triển giao diện web SPA, Vue, React'),
(NULL, 'Mobile Development', 'Phát triển ứng dụng Android / iOS / Flutter'),
(NULL, 'DevOps & Cloud', 'Hạ tầng Cloud, Docker, CI/CD pipelines'),
(NULL, 'Data Engineering', 'Xử lý dữ liệu lớn, ETL, Data Warehouse'),
(NULL, 'AI & Machine Learning', 'Mô hình Trí tuệ nhân tạo và học máy'),
(NULL, 'QA & Software Testing', 'Kiểm thử phần mềm tự động và thủ công'),
(NULL, 'Cyber Security', 'Bảo mật thông tin và an toàn hệ thống'),
(NULL, 'UI/UX Design', 'Thiết kế trải nghiệm và giao diện người dùng');
