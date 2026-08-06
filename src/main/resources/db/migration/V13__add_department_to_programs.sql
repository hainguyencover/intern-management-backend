-- V13__add_department_to_programs.sql -- Add department_id column to programs table
ALTER TABLE programs
ADD COLUMN department_id BIGINT NULL
AFTER id;
-- Add Foreign Key constraint to departments
ALTER TABLE programs
ADD CONSTRAINT fk_programs_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE
SET NULL;
-- Create composite index matching Entity definition
CREATE INDEX idx_programs_department_status ON programs (department_id, status);