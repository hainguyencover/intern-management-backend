-- V49__add_analytics_performance_indexes.sql
-- Add targeted indexes to optimize multi-tenant Analytics & BI queries

CREATE INDEX idx_intern_profiles_tenant_status ON intern_profiles (tenant_id, status);
CREATE INDEX idx_intern_profiles_tenant_created ON intern_profiles (tenant_id, created_at);
