-- V4__add_intern_profile_fields.sql
-- Add missing columns to intern_profiles to match database-structure.md
ALTER TABLE intern_profiles
    ADD COLUMN student_code VARCHAR(50) NULL,
    ADD COLUMN phone VARCHAR(50) NULL,
    ADD COLUMN gpa DOUBLE NULL,
    ADD COLUMN cv_url VARCHAR(1000) NULL;

-- Optional: create indexes that may be useful for filtering/searching
CREATE INDEX IF NOT EXISTS idx_intern_profiles_university ON intern_profiles (university);
CREATE INDEX IF NOT EXISTS idx_intern_profiles_major ON intern_profiles (major);
CREATE INDEX IF NOT EXISTS idx_intern_profiles_student_code ON intern_profiles (student_code);

