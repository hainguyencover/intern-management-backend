-- Flyway Migration V65: Add unassign mentor fields and indexes to mentor_assignments

ALTER TABLE mentor_assignments
    ADD COLUMN unassigned_at DATETIME NULL AFTER ended_at,
    ADD COLUMN unassigned_by BIGINT NULL AFTER unassigned_at,
    ADD COLUMN unassign_reason VARCHAR(500) NULL AFTER unassigned_by;

-- Add FK constraint for unassigned_by
ALTER TABLE mentor_assignments
    ADD CONSTRAINT fk_assignment_unassigned_by
    FOREIGN KEY (unassigned_by) REFERENCES users(id) ON DELETE SET NULL;

-- Add index for unassigned_by query optimization
CREATE INDEX idx_assignment_tenant_unassigned ON mentor_assignments (tenant_id, unassigned_by);
