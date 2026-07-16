-- V4__harden.sql 课次 package_id / 软删除 / 评价索引等加固

ALTER TABLE lessons ADD COLUMN package_id BIGINT NULL AFTER class_id;
ALTER TABLE lessons ADD INDEX idx_lessons_package_id (package_id);

UPDATE lessons l
    JOIN classes c ON l.class_id = c.id
SET l.package_id = c.package_id;

ALTER TABLE enrollments ADD COLUMN paid_amount DECIMAL(12, 2) NULL;

ALTER TABLE course_packages ADD COLUMN deleted_at DATETIME NULL;

ALTER TABLE students ADD COLUMN deleted_at DATETIME NULL;

ALTER TABLE lesson_attendees ADD INDEX idx_attendee_student_rated (student_id, teacher_rating);
