-- Update Programs status from DRAFT to ACTIVE
UPDATE programs SET status = 'ACTIVE' WHERE status = 'DRAFT';
