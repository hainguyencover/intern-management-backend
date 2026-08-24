ALTER TABLE attendances
    ADD COLUMN source_type VARCHAR(30) NULL DEFAULT 'MANUAL',
    ADD COLUMN external_event_id VARCHAR(255) NULL;
