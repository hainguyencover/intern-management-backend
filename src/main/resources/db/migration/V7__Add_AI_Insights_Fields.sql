-- Migration for Phase 13 AI Integration
ALTER TABLE weekly_reports
ADD COLUMN sentiment_label VARCHAR(20),
ADD COLUMN sentiment_score DOUBLE;

ALTER TABLE intern_profiles
ADD COLUMN cv_skills VARCHAR(1000),
ADD COLUMN cv_score INT,
ADD COLUMN cv_summary TEXT;
