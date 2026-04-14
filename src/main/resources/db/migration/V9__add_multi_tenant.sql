-- V9__add_multi_tenant.sql
-- Multi-tenant foundation: tenants table + tenant_id on ALL entity tables
SET NAMES utf8mb4;

-- =========================
-- TENANTS TABLE
-- =========================
CREATE TABLE tenants (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50)   NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    domain      VARCHAR(255)  NULL,
    is_active   TINYINT(1)    NOT NULL DEFAULT 1,
    tenant_id   BIGINT        NOT NULL DEFAULT 1,
    audit_created_by VARCHAR(255) NULL,
    audit_updated_by VARCHAR(255) NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenants_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Seed default tenant
INSERT INTO tenants (code, name, is_active, tenant_id) VALUES ('DEFAULT', 'Default Organization', 1, 1);

-- =========================
-- ADD tenant_id TO ALL TABLES
-- =========================

-- users
ALTER TABLE users ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE users SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE users MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_users_tenant ON users (tenant_id, id);

-- roles
ALTER TABLE roles ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE roles SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE roles MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE roles ADD CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_roles_tenant ON roles (tenant_id, id);

-- permissions
ALTER TABLE permissions ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE permissions SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE permissions MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE permissions ADD CONSTRAINT fk_permissions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_permissions_tenant ON permissions (tenant_id, id);

-- departments
ALTER TABLE departments ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE departments SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE departments MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE departments ADD CONSTRAINT fk_departments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_departments_tenant ON departments (tenant_id, id);

-- mentors
ALTER TABLE mentors ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE mentors SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE mentors MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE mentors ADD CONSTRAINT fk_mentors_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_mentors_tenant ON mentors (tenant_id, id);

-- intern_profiles
ALTER TABLE intern_profiles ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE intern_profiles SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE intern_profiles MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE intern_profiles ADD CONSTRAINT fk_intern_profiles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_intern_profiles_tenant ON intern_profiles (tenant_id, id);

-- intern_documents
ALTER TABLE intern_documents ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE intern_documents SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE intern_documents MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE intern_documents ADD CONSTRAINT fk_intern_documents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_intern_documents_tenant ON intern_documents (tenant_id, id);

-- applications
ALTER TABLE applications ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE applications SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE applications MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE applications ADD CONSTRAINT fk_applications_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_applications_tenant ON applications (tenant_id, id);

-- application_reviews
ALTER TABLE application_reviews ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE application_reviews SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE application_reviews MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE application_reviews ADD CONSTRAINT fk_application_reviews_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_application_reviews_tenant ON application_reviews (tenant_id, id);

-- internship_contracts
ALTER TABLE internship_contracts ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE internship_contracts SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE internship_contracts MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE internship_contracts ADD CONSTRAINT fk_internship_contracts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_internship_contracts_tenant ON internship_contracts (tenant_id, id);

-- programs
ALTER TABLE programs ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE programs SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE programs MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE programs ADD CONSTRAINT fk_programs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_programs_tenant ON programs (tenant_id, id);

-- program_groups
ALTER TABLE program_groups ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE program_groups SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE program_groups MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE program_groups ADD CONSTRAINT fk_program_groups_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_program_groups_tenant ON program_groups (tenant_id, id);

-- group_members
ALTER TABLE group_members ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE group_members SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE group_members MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE group_members ADD CONSTRAINT fk_group_members_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_group_members_tenant ON group_members (tenant_id, id);

-- tasks
ALTER TABLE tasks ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE tasks SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE tasks MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_tasks_tenant ON tasks (tenant_id, id);

-- task_updates
ALTER TABLE task_updates ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE task_updates SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE task_updates MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE task_updates ADD CONSTRAINT fk_task_updates_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_task_updates_tenant ON task_updates (tenant_id, id);

-- evaluations
ALTER TABLE evaluations ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE evaluations SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE evaluations MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE evaluations ADD CONSTRAINT fk_evaluations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_evaluations_tenant ON evaluations (tenant_id, id);

-- attendances
ALTER TABLE attendances ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE attendances SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE attendances MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE attendances ADD CONSTRAINT fk_attendances_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_attendances_tenant ON attendances (tenant_id, id);

-- support_tickets
ALTER TABLE support_tickets ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE support_tickets SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE support_tickets MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE support_tickets ADD CONSTRAINT fk_support_tickets_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_support_tickets_tenant ON support_tickets (tenant_id, id);

-- ticket_comments
ALTER TABLE ticket_comments ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE ticket_comments SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE ticket_comments MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE ticket_comments ADD CONSTRAINT fk_ticket_comments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_ticket_comments_tenant ON ticket_comments (tenant_id, id);

-- notifications
ALTER TABLE notifications ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE notifications SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE notifications MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_notifications_tenant ON notifications (tenant_id, id);

-- weekly_reports
ALTER TABLE weekly_reports ADD COLUMN tenant_id BIGINT NULL AFTER id;
UPDATE weekly_reports SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE weekly_reports MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE weekly_reports ADD CONSTRAINT fk_weekly_reports_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
CREATE INDEX idx_weekly_reports_tenant ON weekly_reports (tenant_id, id);

-- audit_logs
ALTER TABLE audit_logs ADD COLUMN tenant_id BIGINT NULL;
UPDATE audit_logs SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE audit_logs MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;

-- backup_jobs
ALTER TABLE backup_jobs ADD COLUMN tenant_id BIGINT NULL;
UPDATE backup_jobs SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE backup_jobs MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;

-- refresh_tokens
ALTER TABLE refresh_tokens ADD COLUMN tenant_id BIGINT NULL;
UPDATE refresh_tokens SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE refresh_tokens MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;

-- system_configs
ALTER TABLE system_configs ADD COLUMN tenant_id BIGINT NULL;
UPDATE system_configs SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE system_configs MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;

-- leave_requests
ALTER TABLE leave_requests ADD COLUMN tenant_id BIGINT NULL;
UPDATE leave_requests SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE leave_requests MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;

-- allowances
ALTER TABLE allowances ADD COLUMN tenant_id BIGINT NULL;
UPDATE allowances SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE allowances MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
