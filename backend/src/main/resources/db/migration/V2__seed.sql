-- V2__seed.sql 演示数据（学趣思维 / 启航英语）
-- password_hash = BCrypt('123456')

SET NAMES utf8mb4;

INSERT INTO orgs (id, code, name, status) VALUES
(1, 'org_xuequ',  '学趣思维', 'ACTIVE'),
(2, 'org_qihang', '启航英语', 'ACTIVE');

INSERT INTO campuses (id, org_id, name, address, status) VALUES
(1, 1, '城南校区', '城南路88号', 'ACTIVE'),
(2, 1, '总部',     '中心大道1号', 'ACTIVE'),
(3, 2, '河东校区', '河东大道66号', 'ACTIVE');

-- BCrypt of 123456: $2a$10$MRAVOKqVVBjVd6B01bqPg.Ry8uwxqNl5x7iaKyNLbdmC1y4K35.1S
INSERT INTO users (id, phone, name, password_hash, avatar_text, status) VALUES
(1, '13800000001', '王女士',   '$2a$10$MRAVOKqVVBjVd6B01bqPg.Ry8uwxqNl5x7iaKyNLbdmC1y4K35.1S', '王', 'ACTIVE'),
(2, '13800000002', '李老师',   '$2a$10$MRAVOKqVVBjVd6B01bqPg.Ry8uwxqNl5x7iaKyNLbdmC1y4K35.1S', '李', 'ACTIVE'),
(3, '13800000003', '赵教务',   '$2a$10$MRAVOKqVVBjVd6B01bqPg.Ry8uwxqNl5x7iaKyNLbdmC1y4K35.1S', '赵', 'ACTIVE'),
(4, '13800000004', '陈合伙人', '$2a$10$MRAVOKqVVBjVd6B01bqPg.Ry8uwxqNl5x7iaKyNLbdmC1y4K35.1S', '陈', 'ACTIVE'),
(5, '13800000000', '周总',     '$2a$10$MRAVOKqVVBjVd6B01bqPg.Ry8uwxqNl5x7iaKyNLbdmC1y4K35.1S', '周', 'ACTIVE'),
(6, '13800000040', '启航教务', '$2a$10$MRAVOKqVVBjVd6B01bqPg.Ry8uwxqNl5x7iaKyNLbdmC1y4K35.1S', '启', 'ACTIVE'),
(7, '13800000041', '韩老师',   '$2a$10$MRAVOKqVVBjVd6B01bqPg.Ry8uwxqNl5x7iaKyNLbdmC1y4K35.1S', '韩', 'ACTIVE');

INSERT INTO user_roles (user_id, role, org_id) VALUES
(1, 'PARENT',   NULL),
(2, 'TEACHER',  1),
(3, 'ACADEMIC', 1),
(4, 'PARTNER',  1),
(4, 'ACADEMIC', 1),
(4, 'TEACHER',  1),
(5, 'ADMIN',    1),
(5, 'PARTNER',  1),
(5, 'ACADEMIC', 1),
(5, 'TEACHER',  1),
(6, 'ACADEMIC', 2),
(7, 'TEACHER',  2);

INSERT INTO students (id, org_id, parent_phone, student_name, gender, grade, campus, status) VALUES
(1, 1, '13800000001', '王一诺', '女', '小学三年级', '城南校区', 'ACTIVE'),
(2, 1, '13800000001', '王一然', '男', '小学一年级', '城南校区', 'ACTIVE'),
(3, 2, '13800000001', '王一诺', '女', '小学三年级', '河东校区', 'ACTIVE'),
(4, 1, '13800000008', '张小明', '男', '小学四年级', '城南校区', 'ACTIVE'),
(5, 2, '13800000009', '李思思', '女', '小学二年级', '河东校区', 'ACTIVE');

INSERT INTO course_packages (id, org_id, name, subject, grade, lesson_count, price, status, outline) VALUES
(1, 1, '小学数学思维提升', '数学', '小学3-4年级', 48, 6800.00, 'ON_SHELF', '第1课:数感启蒙;第2课:图形分割;...'),
(2, 1, '硬笔书写课',       '语文', '小学1-3年级', 24, 3200.00, 'ON_SHELF', '第1课:基本笔画;...'),
(3, 1, '少儿编程入门',     '编程', '小学3-6年级', 36, 5600.00, 'ON_SHELF', NULL),
(4, 2, '少儿英语进阶营',   '英语', '小学2-4年级', 48, 7200.00, 'ON_SHELF', 'Unit1:Hello;Unit2:Colors;...'),
(5, 2, '自然拼读基础班',   '英语', '小学1-2年级', 32, 4800.00, 'ON_SHELF', NULL);

INSERT INTO enrollments (id, org_id, student_id, package_id, total_lessons, remain_lessons, status, source) VALUES
(1, 1, 1, 1, 48, 36, 'ACTIVE', '教务代录'),
(2, 1, 2, 2, 24, 20, 'ACTIVE', '教务代录'),
(3, 2, 3, 4, 48, 40, 'ACTIVE', '教务代录'),
(4, 1, 4, 1, 48, 45, 'ACTIVE', '老师代录'),
(5, 2, 5, 5, 32, 28, 'ACTIVE', '家长自助');

INSERT INTO teachers (id, org_id, user_id, name, phone, title, campus, status, subjects) VALUES
(1, 1, 2,    '李老师',   '13800000002', '数学主讲',   '城南校区', 'ACTIVE', '数学'),
(2, 1, 4,    '陈合伙人', '13800000004', '兼职主讲',   '城南校区', 'ACTIVE', '数学,编程'),
(3, 1, 5,    '周总',     '13800000000', '教研统筹',   '总部',     'ACTIVE', '数学,编程'),
(4, 1, NULL, '沈老师',   '13800000006', '书写老师',   '城南校区', 'ACTIVE', '语文'),
(5, 2, 7,    '韩老师',   '13800000041', '英语主讲',   '河东校区', 'ACTIVE', '英语'),
(6, 2, NULL, '刘老师',   '13800000042', '拼读助教',   '河东校区', 'ACTIVE', '英语');

INSERT INTO classes (id, org_id, name, package_id, teacher_id, campus, room, status) VALUES
(1, 1, '数学思维A班', 1, 1, '城南校区', 'A203', 'ACTIVE'),
(2, 1, '硬笔书写B班', 2, 4, '城南校区', 'C102', 'ACTIVE'),
(3, 2, '英语进阶1班', 4, 5, '河东校区', 'E201', 'ACTIVE');

INSERT INTO class_students (class_id, student_id) VALUES
(1, 1),
(1, 4),
(2, 2),
(3, 3),
(3, 5);

INSERT INTO lessons (id, org_id, class_id, teacher_id, lesson_date, start_time, end_time, room, status) VALUES
(1, 1, 1, 1, CURDATE(), '16:00:00', '17:30:00', 'A203', 'SCHEDULED'),
(2, 1, 2, 4, CURDATE(), '15:00:00', '16:00:00', 'C102', 'SCHEDULED'),
(3, 2, 3, 5, CURDATE(), '18:00:00', '19:30:00', 'E201', 'SCHEDULED'),
(4, 1, 1, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '16:00:00', '17:30:00', 'A203', 'SCHEDULED'),
(5, 2, 3, 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '18:00:00', '19:30:00', 'E201', 'SCHEDULED');

INSERT INTO lesson_attendees (lesson_id, student_id, type, home_class_id, enrollment_id, consumed, absent) VALUES
(1, 1, 'REGULAR', 1, 1, 0, 0),
(1, 4, 'REGULAR', 1, 4, 0, 0),
(2, 2, 'REGULAR', 2, 2, 0, 0),
(3, 3, 'REGULAR', 3, 3, 0, 0),
(3, 5, 'REGULAR', 3, 5, 0, 0),
(4, 1, 'REGULAR', 1, 1, 0, 0),
(4, 4, 'REGULAR', 1, 4, 0, 0),
(5, 3, 'REGULAR', 3, 3, 0, 0),
(5, 5, 'REGULAR', 3, 5, 0, 0);

INSERT INTO orders (id, org_id, student_id, package_id, campus, amount_total, amount_paid, amount_refund, status, channel, partner_user_id, remark) VALUES
(1, 1, 1, 1, '城南校区', 6800.00, 6800.00, 0.00, 'PAID',    '线下', 4, '一诺数学思维'),
(2, 1, 2, 2, '城南校区', 3200.00, 3000.00, 0.00, 'PARTIAL', '线下', 4, '一然硬笔'),
(3, 2, 3, 4, '河东校区', 7200.00, 7200.00, 0.00, 'PAID',    '线上', NULL, '一诺英语进阶'),
(4, 1, 4, 1, '城南校区', 6800.00, 0.00,    0.00, 'PENDING', '线下', 5, '待收款'),
(5, 2, 5, 5, '河东校区', 4800.00, 4800.00, 200.00, 'REFUNDED', '线上', NULL, '部分退费演示');

INSERT INTO phone_whitelist (org_id, phone, parent_name, note) VALUES
(1, '13800000001', '王女士', '学趣完整导入示例'),
(2, '13800000001', '王女士', '启航英语报读'),
(1, '13800000008', '张妈妈', '城南转介绍'),
(2, '13800000009', '李妈妈', '启航学员家长');
