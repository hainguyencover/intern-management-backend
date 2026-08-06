-- V16__add_candidate_role.sql
-- Add CANDIDATE role using modern non-deprecated alias syntax
INSERT INTO roles(code, name) 
VALUES ('CANDIDATE', 'Candidate') AS new_role 
ON DUPLICATE KEY UPDATE name = new_role.name;
