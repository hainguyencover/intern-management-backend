-- V20__fix_notification_audit_columns.sql
-- Add missing audit and tenant columns to notification_preferences and email_queue tables using standard MySQL syntax

ALTER TABLE notification_preferences ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE notification_preferences ADD COLUMN audit_created_by VARCHAR(255);
ALTER TABLE notification_preferences ADD COLUMN audit_updated_by VARCHAR(255);

ALTER TABLE email_queue ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE email_queue ADD COLUMN audit_created_by VARCHAR(255);
ALTER TABLE email_queue ADD COLUMN audit_updated_by VARCHAR(255);
