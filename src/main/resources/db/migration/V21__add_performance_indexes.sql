-- ================================================================================
-- V21: Add Performance Indexes Based on Phase 2 EXPLAIN ANALYZE Baseline Evidence
-- Target Bottlenecks:
-- 1. Full Table Scan on intern_profiles (student_code) & users (full_name) in keyword search
-- 2. Full Table Scan on weekly_reports (intern_id) in report summary aggregation
-- 3. Full Table Scan on evaluations (intern_id, score) in evaluation stats
-- ================================================================================

-- Multi-Tenant Composite Indexes for Keyword Search & Covering Index Support
CREATE INDEX idx_intern_tenant_student_code ON intern_profiles(tenant_id, student_code);
CREATE INDEX idx_users_tenant_full_name ON users(tenant_id, full_name);

-- Composite Index for Weekly Reports Aggregation by Tenant, Intern & Status
CREATE INDEX idx_weekly_reports_tenant_intern_status ON weekly_reports(tenant_id, intern_id, status);

-- Composite Index for Evaluation Aggregation by Tenant, Intern & Score
CREATE INDEX idx_evaluations_tenant_intern_score ON evaluations(tenant_id, intern_id, score);

-- Index for University & Major Statistics Projection
CREATE INDEX idx_intern_university_major ON intern_profiles(tenant_id, university, major);
