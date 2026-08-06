-- V18__add_status_to_intern_profiles.sql
-- Add status column to intern_profiles table
ALTER TABLE intern_profiles ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ONBOARDING';
