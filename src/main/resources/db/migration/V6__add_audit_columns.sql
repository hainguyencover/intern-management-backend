-- V6__add_audit_columns.sql
-- Add audit_created_by and audit_updated_by columns to all business tables.
-- Renamed to avoid collision with existing relation columns like 'created_by' (BIGINT) in tasks/support_tickets.

-- AUTH / USERS
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

-- MASTER DATA
ALTER TABLE departments
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE mentors
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

-- INTERN
ALTER TABLE intern_profiles
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE intern_documents
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

-- APPLICATIONS
ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE application_reviews
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE internship_contracts
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

-- PROGRAMS / GROUPS
ALTER TABLE programs
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE program_groups
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE group_members
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

-- TASKS
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

-- TASK UPDATES
ALTER TABLE task_updates
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

-- EVALUATION / ATTENDANCE
ALTER TABLE evaluations
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE attendances
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

-- SUPPORT / NOTIFICATION
ALTER TABLE support_tickets
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE ticket_comments
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

-- RBAC
ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;

ALTER TABLE backup_jobs
    ADD COLUMN IF NOT EXISTS audit_created_by VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS audit_updated_by VARCHAR(255) NULL;
