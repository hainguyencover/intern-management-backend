-- V6__add_audit_columns.sql
-- Add audit_created_by and audit_updated_by columns to all business tables.
-- Renamed to avoid collision with existing relation columns like 'created_by' (BIGINT) in tasks/support_tickets.

-- AUTH / USERS
ALTER TABLE users
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE roles
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

-- MASTER DATA
ALTER TABLE departments
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE mentors
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

-- INTERN
ALTER TABLE intern_profiles
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE intern_documents
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

-- APPLICATIONS
ALTER TABLE applications
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE application_reviews
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE internship_contracts
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

-- PROGRAMS / GROUPS
ALTER TABLE programs
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE program_groups
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE group_members
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

-- TASKS
ALTER TABLE tasks
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

-- TASK UPDATES
ALTER TABLE task_updates
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

-- EVALUATION / ATTENDANCE
ALTER TABLE evaluations
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE attendances
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

-- SUPPORT / NOTIFICATION
ALTER TABLE support_tickets
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE ticket_comments
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE notifications
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

-- RBAC
ALTER TABLE permissions
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE audit_logs
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;

ALTER TABLE backup_jobs
    ADD COLUMN audit_created_by VARCHAR(255) NULL,
    ADD COLUMN audit_updated_by VARCHAR(255) NULL;
