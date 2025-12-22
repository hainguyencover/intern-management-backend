-- V1__init.sql
-- MySQL 8.x | InnoDB | utf8mb4
SET NAMES utf8mb4;
SET time_zone = '+07:00';

-- =========================
-- AUTH / USERS / ROLES
-- =========================
CREATE TABLE roles
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_roles_code UNIQUE (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE users
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(50)  NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- =========================
-- MASTER DATA
-- =========================
CREATE TABLE departments
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_departments_code UNIQUE (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE mentors
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    department_id BIGINT       NULL,
    title         VARCHAR(255) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_mentors_user_id UNIQUE (user_id),
    CONSTRAINT fk_mentors_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_mentors_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- =========================
-- INTERN
-- =========================
CREATE TABLE intern_profiles
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    dob        DATE         NULL,
    university VARCHAR(255) NULL,
    major      VARCHAR(255) NULL,
    address    VARCHAR(500) NULL,
    start_date DATE         NULL,
    end_date   DATE         NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_intern_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_intern_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE intern_documents
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    intern_id   BIGINT        NOT NULL,
    type        VARCHAR(50)   NOT NULL,
    file_url    VARCHAR(1000) NOT NULL,
    status      VARCHAR(30)   NOT NULL,
    uploaded_at DATETIME      NULL,
    reviewed_by BIGINT        NULL,
    reviewed_at DATETIME      NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_intern_documents_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_intern_documents_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_intern_documents_type CHECK (type IN ('CV', 'APPLICATION_LETTER', 'OTHER')),
    CONSTRAINT ck_intern_documents_status CHECK (status IN ('UPLOADED', 'APPROVED', 'REJECTED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- =========================
-- APPLICATIONS
-- =========================
CREATE TABLE applications
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    intern_id  BIGINT        NOT NULL,
    position   VARCHAR(255)  NULL,
    applied_at DATETIME      NULL,
    status     VARCHAR(30)   NOT NULL,
    note       VARCHAR(1000) NULL,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_applications_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT ck_applications_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CONTRACT_SENT',
                                                        'CONTRACT_SIGNED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_applications_status ON applications (status);

CREATE TABLE application_reviews
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT        NOT NULL,
    reviewer_id    BIGINT        NOT NULL,
    decision       VARCHAR(20)   NOT NULL,
    comment        VARCHAR(1000) NULL,
    decided_at     DATETIME      NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_application_reviews_app FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_application_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_application_reviews_decision CHECK (decision IN ('APPROVE', 'REJECT'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE internship_contracts
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT        NOT NULL,
    file_url       VARCHAR(1000) NOT NULL,
    signed_at      DATETIME      NULL,
    status         VARCHAR(30)   NOT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_contracts_application_id UNIQUE (application_id),
    CONSTRAINT fk_contracts_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT ck_contracts_status CHECK (status IN ('SENT', 'SIGNED', 'CANCELLED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- =========================
-- PROGRAMS / GROUPS
-- =========================
CREATE TABLE programs
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(255)  NOT NULL,
    description VARCHAR(2000) NULL,
    start_date  DATE          NULL,
    end_date    DATE          NULL,
    status      VARCHAR(20)   NOT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_programs_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_programs_status ON programs (status);

CREATE TABLE program_groups
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    program_id    BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    department_id BIGINT       NULL,
    mentor_id     BIGINT       NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_program_groups_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE CASCADE,
    CONSTRAINT fk_program_groups_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_program_groups_mentor FOREIGN KEY (mentor_id) REFERENCES mentors (id) ON DELETE SET NULL,
    CONSTRAINT ck_program_groups_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE group_members
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id   BIGINT   NOT NULL,
    intern_id  BIGINT   NOT NULL,
    joined_at  DATETIME NULL,
    left_at    DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES program_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_members_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- =========================
-- TASKS
-- =========================
CREATE TABLE tasks
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id    BIGINT        NOT NULL,
    title       VARCHAR(255)  NOT NULL,
    description VARCHAR(4000) NULL,
    due_date    DATETIME      NULL,
    status      VARCHAR(30)   NOT NULL,
    created_by  BIGINT        NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_group FOREIGN KEY (group_id) REFERENCES program_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_tasks_status CHECK (status IN
                                      ('OPEN', 'IN_PROGRESS', 'SUBMITTED', 'APPROVED', 'NEEDS_CHANGES', 'DONE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_tasks_status ON tasks (status);

CREATE TABLE task_updates
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id          BIGINT        NOT NULL,
    intern_id        BIGINT        NOT NULL,
    progress_percent INT           NULL,
    content          VARCHAR(4000) NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_updates_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_updates_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT ck_task_updates_progress CHECK (progress_percent IS NULL OR (progress_percent BETWEEN 0 AND 100))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- =========================
-- EVALUATION / ATTENDANCE
-- =========================
CREATE TABLE evaluations
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    intern_id  BIGINT        NOT NULL,
    mentor_id  BIGINT        NOT NULL,
    period     VARCHAR (50) NULL,
    score      INT           NULL,
    comment    VARCHAR(2000) NULL,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_evaluations_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_evaluations_mentor FOREIGN KEY (mentor_id) REFERENCES mentors (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE attendances
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    intern_id     BIGINT        NOT NULL,
    date          DATE          NOT NULL,
    check_in      DATETIME      NULL,
    check_out     DATETIME      NULL,
    total_minutes INT           NULL,
    note          VARCHAR(1000) NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendances_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_attendances_date ON attendances (date);

-- =========================
-- SUPPORT / NOTIFICATION
-- =========================
CREATE TABLE support_tickets
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_by BIGINT        NOT NULL,
    category   VARCHAR(30)   NOT NULL,
    title      VARCHAR(255)  NOT NULL,
    content    VARCHAR(4000) NOT NULL,
    status     VARCHAR(30)   NOT NULL,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_tickets_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_support_tickets_category CHECK (category IN ('CERTIFICATE', 'DOCUMENT', 'GENERAL', 'OTHER')),
    CONSTRAINT ck_support_tickets_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE ticket_comments
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id  BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    content    VARCHAR(4000) NOT NULL,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_comments_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE notifications
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT        NOT NULL,
    type       VARCHAR(30)   NOT NULL,
    title      VARCHAR(255)  NOT NULL,
    content    VARCHAR(4000) NOT NULL,
    is_read    TINYINT(1)    NOT NULL DEFAULT 0,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_notifications_type CHECK (type IN ('SYSTEM', 'EMAIL', 'TASK', 'APPLICATION', 'OTHER'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
