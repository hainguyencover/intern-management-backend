-- V15__add_lockout_fields_to_users.sql
-- Add account lockout fields to users table
ALTER TABLE users ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN lock_time DATETIME NULL;
