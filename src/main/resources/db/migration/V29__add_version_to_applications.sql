-- V29__add_version_to_applications.sql
-- Migration to add version column to applications table for Optimistic Locking

ALTER TABLE applications ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
