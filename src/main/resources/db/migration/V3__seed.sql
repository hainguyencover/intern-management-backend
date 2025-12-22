-- V3__seed.sql

-- =========
-- ROLES
-- =========
INSERT INTO roles(code, name)
VALUES ('ADMIN', 'System Admin'),
       ('HR', 'HR'),
       ('MENTOR', 'Mentor'),
       ('INTERN', 'Intern') ON DUPLICATE KEY
UPDATE name =
VALUES (name);

-- =========
-- PERMISSIONS (MVP)
-- =========
INSERT INTO permissions(code, name, description)
VALUES
-- Admin / RBAC
('ADMIN_USER_MANAGE', 'Manage users', 'Create/update/lock users'),
('ADMIN_ROLE_MANAGE', 'Manage roles', 'Create/update roles'),
('ADMIN_PERMISSION_MANAGE', 'Manage permissions', 'Create/update permissions and mapping'),

-- Intern
('INTERN_READ', 'Read intern profiles', 'View intern profiles'),
('INTERN_CREATE', 'Create intern profiles', 'Create intern profiles'),
('INTERN_UPDATE', 'Update intern profiles', 'Update intern profiles'),

-- Application
('APPLICATION_READ', 'Read applications', 'View applications'),
('APPLICATION_SUBMIT', 'Submit application', 'Submit application'),
('APPLICATION_REVIEW', 'Review application', 'Approve/Reject application'),

-- Program/Group
('PROGRAM_MANAGE', 'Manage programs', 'Create/update programs'),
('GROUP_MANAGE', 'Manage groups', 'Create/update groups and members'),

-- Task
('TASK_CREATE', 'Create tasks', 'Create tasks for group'),
('TASK_ASSIGN', 'Assign tasks', 'Assign/Manage tasks'),
('TASK_UPDATE', 'Update progress', 'Intern update task progress'),
('TASK_REVIEW', 'Review task', 'Approve/Request changes'),

-- Attendance
('ATTENDANCE_VIEW', 'View attendance', 'View attendance report'),
('ATTENDANCE_CHECKIN', 'Check-in/out', 'Intern check-in/out'),

-- Support
('SUPPORT_CREATE', 'Create ticket', 'Create support ticket'),
('SUPPORT_HANDLE', 'Handle ticket', 'Resolve/Close ticket'),

-- Notification
('NOTIFICATION_READ', 'Read notifications', 'View notifications') ON DUPLICATE KEY
UPDATE
    name =
VALUES (name), description =
VALUES (description);

-- =========
-- ROLE -> PERMISSIONS
-- =========
-- ADMIN gets all permissions
INSERT
IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p
WHERE r.code = 'ADMIN';

-- HR typical permissions
INSERT
IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p
WHERE r.code = 'HR'
  AND p.code IN (
                 'INTERN_READ', 'INTERN_CREATE', 'INTERN_UPDATE',
                 'APPLICATION_READ', 'APPLICATION_REVIEW',
                 'PROGRAM_MANAGE', 'GROUP_MANAGE',
                 'TASK_CREATE', 'TASK_ASSIGN', 'TASK_REVIEW',
                 'ATTENDANCE_VIEW',
                 'SUPPORT_HANDLE',
                 'NOTIFICATION_READ'
    );

-- MENTOR typical permissions
INSERT
IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p
WHERE r.code = 'MENTOR'
  AND p.code IN (
                 'INTERN_READ',
                 'APPLICATION_READ',
                 'GROUP_MANAGE',
                 'TASK_CREATE', 'TASK_ASSIGN', 'TASK_REVIEW',
                 'ATTENDANCE_VIEW',
                 'SUPPORT_HANDLE',
                 'NOTIFICATION_READ'
    );

-- INTERN typical permissions
INSERT
IGNORE INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p
WHERE r.code = 'INTERN'
  AND p.code IN (
                 'APPLICATION_READ', 'APPLICATION_SUBMIT',
                 'TASK_UPDATE',
                 'ATTENDANCE_CHECKIN',
                 'SUPPORT_CREATE',
                 'NOTIFICATION_READ'
    );

-- =========
-- ADMIN USER (minimal for login test)
-- =========
-- password_hash: bcrypt for "Admin@123" (bạn có thể thay bằng hash của bạn)
INSERT INTO users(email, password_hash, full_name, phone, status)
VALUES ('admin@ims.local',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2',
        'System Administrator',
        NULL,
        'ACTIVE') ON DUPLICATE KEY
UPDATE
    full_name =
VALUES (full_name), status =
VALUES (status);

-- Map ADMIN role to admin user
INSERT
IGNORE INTO user_roles(user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.code = 'ADMIN'
WHERE u.email = 'admin@ims.local';

