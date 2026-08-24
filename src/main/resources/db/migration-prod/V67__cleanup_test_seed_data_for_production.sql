-- Flyway Migration V67: Clean up test seed data from V23 in production
-- This migration is placed in db/migration-prod/ so it ONLY runs when
-- the production Flyway location is configured.
--
-- In application-prod.yml, Flyway locations include both:
--   classpath:db/migration,classpath:db/migration-prod
-- In application-dev.yml, only classpath:db/migration is used,
-- so test seed data from V23 remains available for development.

-- ============================================================================
-- PRODUCTION DATA CLEANUP
-- Remove test/demo data that was seeded by V23
-- Uses targeted DELETE with specific email patterns — safe and idempotent
-- ============================================================================

-- Clean up test support tickets
DELETE st FROM support_tickets st
    INNER JOIN users u ON st.created_by = u.id
    WHERE u.email IN ('intern1@student.com','intern2@student.com','intern3@student.com','intern4@student.com','intern5@student.com');

-- Clean up test leave requests  
DELETE lr FROM leave_requests lr
    INNER JOIN intern_profiles ip ON lr.intern_id = ip.id
    INNER JOIN users u ON ip.user_id = u.id
    WHERE u.email LIKE 'intern%@student.com';

-- Clean up test evaluations
DELETE e FROM evaluations e
    INNER JOIN intern_profiles ip ON e.intern_id = ip.id
    INNER JOIN users u ON ip.user_id = u.id
    WHERE u.email LIKE 'intern%@student.com';

-- Clean up test weekly reports
DELETE wr FROM weekly_reports wr
    INNER JOIN intern_profiles ip ON wr.intern_id = ip.id
    INNER JOIN users u ON ip.user_id = u.id
    WHERE u.email LIKE 'intern%@student.com';

-- Clean up test tasks
DELETE t FROM tasks t WHERE t.title = 'Thiết kế Database ERD & Flyway Migration';

-- Clean up test program groups
DELETE pg FROM program_groups pg WHERE pg.name = 'Group Java Backend 01';

-- Clean up test contracts
DELETE ic FROM internship_contracts ic
    INNER JOIN applications a ON ic.application_id = a.id
    INNER JOIN intern_profiles ip ON a.intern_id = ip.id
    INNER JOIN users u ON ip.user_id = u.id
    WHERE u.email LIKE 'intern%@student.com';

-- Clean up test applications
DELETE a FROM applications a
    INNER JOIN intern_profiles ip ON a.intern_id = ip.id
    INNER JOIN users u ON ip.user_id = u.id
    WHERE u.email LIKE 'intern%@student.com';

-- Clean up test intern profiles
DELETE ip FROM intern_profiles ip
    INNER JOIN users u ON ip.user_id = u.id
    WHERE u.email LIKE 'intern%@student.com';

-- Clean up test mentors
DELETE m FROM mentors m
    INNER JOIN users u ON m.user_id = u.id
    WHERE u.email LIKE 'mentor%@company.com';

-- Clean up test user_roles  
DELETE ur FROM user_roles ur
    INNER JOIN users u ON ur.user_id = u.id
    WHERE u.email LIKE 'hr%@company.com'
       OR u.email LIKE 'mentor%@company.com'
       OR u.email LIKE 'intern%@student.com';

-- Clean up test users (FK dependencies already cleared above)
DELETE FROM users WHERE email LIKE 'hr%@company.com';
DELETE FROM users WHERE email LIKE 'mentor%@company.com';
DELETE FROM users WHERE email LIKE 'intern%@student.com';

-- Clean up test programs
DELETE FROM programs WHERE name IN (
    'Software Engineer Intern 2026 - Batch 1',
    'Frontend Web Developer Intern 2026 - Batch 1',
    'QA & Automation Testing Intern 2026',
    'Mobile App React Native Intern 2026',
    'DevOps & Cloud Systems Intern 2026'
);

-- Clean up test department
DELETE FROM departments WHERE code = 'ENG' AND name = 'Phòng Công nghệ & Phần mềm';
