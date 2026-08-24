-- ================================================================================
-- V26: Ensure Tenant ID Backfill for Intern Profiles & Users
-- Target: Fix multi-tenant isolation fallback so all records default to tenant 1
-- ================================================================================

UPDATE intern_profiles SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE users SET tenant_id = 1 WHERE tenant_id IS NULL;
