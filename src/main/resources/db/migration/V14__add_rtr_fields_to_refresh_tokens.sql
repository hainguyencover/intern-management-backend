-- V14__add_rtr_fields_to_refresh_tokens.sql
-- Add fields to refresh_tokens table to support Refresh Token Rotation (RTR)
ALTER TABLE refresh_tokens ADD COLUMN is_used TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE refresh_tokens ADD COLUMN is_revoked TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE refresh_tokens ADD COLUMN replaced_by_token VARCHAR(255) NULL;
