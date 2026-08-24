CREATE INDEX idx_sync_jobs_connection_status
ON integration_sync_jobs(connection_id, status);

CREATE INDEX idx_sync_items_job_status
ON integration_sync_items(sync_job_id, status);

CREATE INDEX idx_external_mapping_internal
ON external_identity_mappings(internal_id);

CREATE INDEX idx_attendance_event_employee_time
ON attendance_integration_events(
    external_employee_id,
    event_time
);

CREATE INDEX idx_attendance_event_status
ON attendance_integration_events(processing_status);
