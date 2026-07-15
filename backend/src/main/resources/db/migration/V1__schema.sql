-- V1__schema.sql 学管云核心表结构

CREATE TABLE orgs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(64)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_orgs_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE campuses (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id        BIGINT       NOT NULL,
    name          VARCHAR(128) NOT NULL,
    address       VARCHAR(255) NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_campuses_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone          VARCHAR(20)  NOT NULL,
    name           VARCHAR(64)  NULL,
    password_hash  VARCHAR(255) NULL,
    avatar_text    VARCHAR(8)   NULL,
    status         VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    role          VARCHAR(32) NOT NULL COMMENT 'PARENT/TEACHER/ACADEMIC/PARTNER/ADMIN',
    org_id        BIGINT      NULL COMMENT '家长可为空；员工必填',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_roles_user_role_org (user_id, role, org_id),
    INDEX idx_user_roles_org_id (org_id),
    INDEX idx_user_roles_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE students (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id         BIGINT       NOT NULL,
    parent_phone   VARCHAR(20)  NOT NULL,
    student_name   VARCHAR(64)  NOT NULL,
    gender         VARCHAR(16)  NULL,
    grade          VARCHAR(32)  NULL,
    birthday       DATE         NULL,
    campus         VARCHAR(128) NULL,
    remark         VARCHAR(512) NULL,
    status         VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_students_org_phone_name (org_id, parent_phone, student_name),
    INDEX idx_students_org_id (org_id),
    INDEX idx_students_parent_phone (parent_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE course_packages (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id         BIGINT         NOT NULL,
    name           VARCHAR(128)   NOT NULL,
    subject        VARCHAR(64)    NULL,
    grade          VARCHAR(64)    NULL,
    lesson_count   INT            NOT NULL DEFAULT 0,
    price          DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    status         VARCHAR(32)    NOT NULL DEFAULT 'ON_SHELF',
    outline        TEXT           NULL,
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_packages_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE enrollments (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id           BIGINT         NOT NULL,
    student_id       BIGINT         NOT NULL,
    package_id       BIGINT         NOT NULL,
    total_lessons    INT            NOT NULL DEFAULT 0,
    remain_lessons   INT            NOT NULL DEFAULT 0,
    status           VARCHAR(32)    NOT NULL DEFAULT 'ACTIVE',
    source           VARCHAR(64)    NULL COMMENT '教务代录/老师代录/家长自助等',
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enrollments_org_id (org_id),
    INDEX idx_enrollments_student_id (student_id),
    INDEX idx_enrollments_package_id (package_id),
    UNIQUE KEY uk_enrollments_student_package (student_id, package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teachers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id        BIGINT       NOT NULL,
    user_id       BIGINT       NULL,
    name          VARCHAR(64)  NOT NULL,
    phone         VARCHAR(20)  NULL,
    title         VARCHAR(64)  NULL,
    campus        VARCHAR(128) NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    subjects      VARCHAR(255) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_teachers_org_id (org_id),
    INDEX idx_teachers_user_id (user_id),
    INDEX idx_teachers_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classes (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id        BIGINT       NOT NULL,
    name          VARCHAR(128) NOT NULL,
    package_id    BIGINT       NULL,
    teacher_id    BIGINT       NULL,
    campus        VARCHAR(128) NULL,
    room          VARCHAR(64)  NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_classes_org_id (org_id),
    INDEX idx_classes_package_id (package_id),
    INDEX idx_classes_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE class_students (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id      BIGINT   NOT NULL,
    student_id    BIGINT   NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_students (class_id, student_id),
    INDEX idx_class_students_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lessons (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id        BIGINT       NOT NULL,
    class_id      BIGINT       NOT NULL,
    teacher_id    BIGINT       NULL,
    lesson_date   DATE         NOT NULL,
    start_time    TIME         NOT NULL,
    end_time      TIME         NOT NULL,
    room          VARCHAR(64)  NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'SCHEDULED',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_lessons_org_id (org_id),
    INDEX idx_lessons_class_id (class_id),
    INDEX idx_lessons_teacher_id (teacher_id),
    INDEX idx_lessons_date (lesson_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE lesson_attendees (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id              BIGINT       NOT NULL,
    student_id             BIGINT       NOT NULL,
    type                   VARCHAR(32)  NOT NULL DEFAULT 'REGULAR' COMMENT 'REGULAR/MAKEUP',
    home_class_id          BIGINT       NULL,
    enrollment_id          BIGINT       NULL,
    teacher_rating         INT          NULL,
    teacher_comment        VARCHAR(512) NULL,
    student_rating         INT          NULL,
    student_comment        VARCHAR(512) NULL,
    consumed               TINYINT(1)   NOT NULL DEFAULT 0,
    absent                 TINYINT(1)   NOT NULL DEFAULT 0,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lesson_attendees (lesson_id, student_id),
    INDEX idx_lesson_attendees_student_id (student_id),
    INDEX idx_lesson_attendees_enrollment_id (enrollment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id           BIGINT         NOT NULL,
    student_id       BIGINT         NOT NULL,
    package_id       BIGINT         NOT NULL,
    campus           VARCHAR(128)   NULL,
    amount_total     DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    amount_paid      DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    amount_refund    DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    status           VARCHAR(32)    NOT NULL DEFAULT 'PENDING',
    channel          VARCHAR(64)    NULL,
    partner_user_id  BIGINT         NULL,
    remark           VARCHAR(512)   NULL,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_orders_org_id (org_id),
    INDEX idx_orders_student_id (student_id),
    INDEX idx_orders_package_id (package_id),
    INDEX idx_orders_partner_user_id (partner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE phone_whitelist (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id        BIGINT       NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    parent_name   VARCHAR(64)  NULL,
    note          VARCHAR(255) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_phone_whitelist_org_phone (org_id, phone),
    INDEX idx_phone_whitelist_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
