-- Migration to add AI screening results to applications
ALTER TABLE applications
ADD COLUMN ai_score INT,
ADD COLUMN ai_skills VARCHAR(1000),
ADD COLUMN ai_summary TEXT,
ADD COLUMN ai_recommendation TEXT;
