-- ===========================================================================
-- V1__init_schema.sql
-- Student Performance & Attendance Analytics Dashboard — initial schema (PostgreSQL)
-- Column names/lengths are hand-matched against every JPA entity — see the
-- cross-check performed before writing this file.
--
-- Migrated from an original MySQL version of this same migration. Table,
-- column, and constraint names are all unchanged — only MySQL-specific
-- syntax was translated:
--   AUTO_INCREMENT                  -> GENERATED ALWAYS AS IDENTITY
--   ENGINE=InnoDB DEFAULT CHARSET.. -> removed (no Postgres equivalent needed)
--   SET NAMES utf8mb4;              -> removed (MySQL-only statement)
--   DATETIME(6)                     -> TIMESTAMPTZ (see note below)
--
-- DATETIME(6) -> TIMESTAMPTZ, not TIMESTAMP: every DATETIME(6) column here
-- backs a java.time.Instant field. Hibernate 6's PostgreSQLDialect maps
-- Instant to "timestamp with time zone" by default (Postgres stores it as
-- an unambiguous UTC instant internally regardless of session timezone,
-- which is the correct semantic match for Instant — MySQL's DATETIME has
-- no such concept, which is why the original column type looks different
-- even though the Java type didn't change). Since ddl-auto is "validate",
-- this has to match what Hibernate actually expects at startup, not just
-- be "a reasonable timestamp type".
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- Auth
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username   VARCHAR(60)   NOT NULL,
    email      VARCHAR(150)  NOT NULL,
    password   VARCHAR(255)  NOT NULL,
    role       VARCHAR(20)   NOT NULL,
    full_name  VARCHAR(120)  NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- ---------------------------------------------------------------------------
-- Reference data
-- ---------------------------------------------------------------------------

CREATE TABLE academic_session (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label      VARCHAR(20) NOT NULL,
    is_current BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_academic_session_label UNIQUE (label)
);

CREATE TABLE term (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(20) NOT NULL,
    sort_order INT         NOT NULL,
    is_current BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_term_name UNIQUE (name)
);

CREATE TABLE school_class (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    class_code      VARCHAR(20) NOT NULL,
    name            VARCHAR(60) NOT NULL,
    level           VARCHAR(40) NOT NULL,
    form_teacher_id BIGINT,
    CONSTRAINT uk_school_class_code UNIQUE (class_code)
);

CREATE TABLE subject (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_code VARCHAR(20) NOT NULL,
    name         VARCHAR(80) NOT NULL,
    CONSTRAINT uk_subject_code UNIQUE (subject_code)
);

-- ---------------------------------------------------------------------------
-- Staff
-- ---------------------------------------------------------------------------

CREATE TABLE teacher (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    teacher_code VARCHAR(20)  NOT NULL,
    name         VARCHAR(120) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    phone        VARCHAR(30),
    status       VARCHAR(20)  NOT NULL DEFAULT 'Active',
    joined_date  DATE,
    -- Nullable on purpose: a teacher can exist with no login access yet.
    user_id      BIGINT,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_teacher_code UNIQUE (teacher_code),
    CONSTRAINT uk_teacher_email UNIQUE (email),
    CONSTRAINT uk_teacher_user_id UNIQUE (user_id),
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE teacher_class (
    teacher_id BIGINT NOT NULL,
    class_id   BIGINT NOT NULL,
    PRIMARY KEY (teacher_id, class_id),
    CONSTRAINT fk_teacher_class_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
    CONSTRAINT fk_teacher_class_class FOREIGN KEY (class_id) REFERENCES school_class(id) ON DELETE CASCADE
);
CREATE INDEX idx_teacher_class_class_id ON teacher_class(class_id);

-- Added now that `teacher` exists — school_class was created earlier in this file.
ALTER TABLE school_class
    ADD CONSTRAINT fk_school_class_form_teacher FOREIGN KEY (form_teacher_id) REFERENCES teacher(id) ON DELETE SET NULL;

CREATE TABLE teacher_subject (
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (teacher_id, subject_id),
    CONSTRAINT fk_teacher_subject_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
    CONSTRAINT fk_teacher_subject_subject FOREIGN KEY (subject_id) REFERENCES subject(id) ON DELETE CASCADE
);
CREATE INDEX idx_teacher_subject_subject_id ON teacher_subject(subject_id);

-- ---------------------------------------------------------------------------
-- Students
-- ---------------------------------------------------------------------------

CREATE TABLE student (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_code   VARCHAR(20)  NOT NULL,
    name           VARCHAR(120) NOT NULL,
    gender         VARCHAR(10)  NOT NULL,
    class_id       BIGINT       NOT NULL,
    dob            DATE,
    guardian_name  VARCHAR(120),
    guardian_phone VARCHAR(30),
    address        VARCHAR(200),
    status         VARCHAR(20)  NOT NULL DEFAULT 'Active',
    admitted_date  DATE,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_student_code UNIQUE (student_code),
    CONSTRAINT fk_student_class FOREIGN KEY (class_id) REFERENCES school_class(id)
);
CREATE INDEX idx_student_class_id ON student(class_id);

-- ---------------------------------------------------------------------------
-- Academic records
-- ---------------------------------------------------------------------------

CREATE TABLE result (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id          BIGINT      NOT NULL,
    class_id            BIGINT      NOT NULL,
    subject_id          BIGINT      NOT NULL,
    academic_session_id BIGINT      NOT NULL,
    term_id             BIGINT      NOT NULL,
    ca                  INT         NOT NULL,
    exam                INT         NOT NULL,
    total               INT         NOT NULL,
    grade               VARCHAR(2)  NOT NULL,
    comment             VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_result_unique_entry UNIQUE (student_id, subject_id, academic_session_id, term_id),
    CONSTRAINT fk_result_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    CONSTRAINT fk_result_class FOREIGN KEY (class_id) REFERENCES school_class(id),
    CONSTRAINT fk_result_subject FOREIGN KEY (subject_id) REFERENCES subject(id),
    CONSTRAINT fk_result_session FOREIGN KEY (academic_session_id) REFERENCES academic_session(id),
    CONSTRAINT fk_result_term FOREIGN KEY (term_id) REFERENCES term(id),
    CONSTRAINT chk_result_ca CHECK (ca BETWEEN 0 AND 40),
    CONSTRAINT chk_result_exam CHECK (exam BETWEEN 0 AND 60)
);
CREATE INDEX idx_result_session_term ON result(academic_session_id, term_id);
CREATE INDEX idx_result_class_session_term ON result(class_id, academic_session_id, term_id);
CREATE INDEX idx_result_subject_session_term ON result(subject_id, academic_session_id, term_id);

CREATE TABLE attendance (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id          BIGINT      NOT NULL,
    class_id            BIGINT      NOT NULL,
    academic_session_id BIGINT      NOT NULL,
    term_id             BIGINT      NOT NULL,
    date                DATE        NOT NULL,
    status              VARCHAR(10) NOT NULL,
    remarks             VARCHAR(200),
    created_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_class FOREIGN KEY (class_id) REFERENCES school_class(id),
    CONSTRAINT fk_attendance_session FOREIGN KEY (academic_session_id) REFERENCES academic_session(id),
    CONSTRAINT fk_attendance_term FOREIGN KEY (term_id) REFERENCES term(id)
);
CREATE INDEX idx_attendance_session_term ON attendance(academic_session_id, term_id);
CREATE INDEX idx_attendance_class_session_term ON attendance(class_id, academic_session_id, term_id);
CREATE INDEX idx_attendance_student_session_term ON attendance(student_id, academic_session_id, term_id);

CREATE TABLE teacher_comment (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id          BIGINT        NOT NULL,
    teacher_id          BIGINT        NOT NULL,
    academic_session_id BIGINT        NOT NULL,
    term_id             BIGINT        NOT NULL,
    comment             VARCHAR(1000) NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_teacher_comment_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    CONSTRAINT fk_teacher_comment_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    CONSTRAINT fk_teacher_comment_session FOREIGN KEY (academic_session_id) REFERENCES academic_session(id),
    CONSTRAINT fk_teacher_comment_term FOREIGN KEY (term_id) REFERENCES term(id)
);
CREATE INDEX idx_teacher_comment_student_id ON teacher_comment(student_id);

-- ---------------------------------------------------------------------------
-- Notifications & activity feed
-- ---------------------------------------------------------------------------

CREATE TABLE notification (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type    VARCHAR(40)  NOT NULL,
    message VARCHAR(500) NOT NULL,
    date    DATE         NOT NULL,
    is_read BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE activity_log (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type       VARCHAR(40)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    actor      VARCHAR(60)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_activity_log_created_at ON activity_log(created_at);
