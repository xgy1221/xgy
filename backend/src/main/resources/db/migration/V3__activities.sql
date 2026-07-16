-- V3__activities.sql 活动报名

CREATE TABLE activities (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id           BIGINT         NOT NULL,
    title            VARCHAR(128)   NOT NULL,
    category         VARCHAR(64)    NULL COMMENT '学科竞赛/趣味赛事/口语赛事/作品赛/主题赛事',
    cover_tone       VARCHAR(32)    NULL DEFAULT 'teal',
    campus           VARCHAR(128)   NULL,
    address          VARCHAR(255)   NULL,
    start_date       DATE           NOT NULL,
    start_time       TIME           NOT NULL,
    end_time         TIME           NOT NULL,
    enroll_deadline  DATE           NOT NULL,
    capacity         INT            NOT NULL DEFAULT 0,
    fee              DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    target_grade     VARCHAR(64)    NULL,
    summary          TEXT           NULL,
    highlights       TEXT           NULL COMMENT 'JSON array string or comma-separated',
    gallery          TEXT           NULL,
    recap            TEXT           NULL,
    published        TINYINT(1)     NOT NULL DEFAULT 1,
    status           VARCHAR(32)    NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_activities_org_id (org_id),
    INDEX idx_activities_start_date (start_date),
    INDEX idx_activities_org_start (org_id, start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE activity_signups (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id         BIGINT       NOT NULL,
    activity_id    BIGINT       NOT NULL,
    student_id     BIGINT       NOT NULL,
    student_name   VARCHAR(64)  NULL,
    parent_phone   VARCHAR(20)  NOT NULL,
    status         VARCHAR(32)  NOT NULL DEFAULT 'CONFIRMED' COMMENT 'CONFIRMED/CANCELLED',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_activity_signups_org_id (org_id),
    INDEX idx_activity_signups_activity_id (activity_id),
    INDEX idx_activity_signups_student_id (student_id),
    INDEX idx_activity_signups_parent_phone (parent_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- 唯一约束不包含 CANCELLED：服务层保证同一 (activity_id, student_id) 仅一条 CONFIRMED

-- 学趣思维 org=1：5 场（含即将开始与已结束）
INSERT INTO activities (
    id, org_id, title, category, cover_tone, campus, address,
    start_date, start_time, end_time, enroll_deadline,
    capacity, fee, target_grade, summary, highlights, gallery, recap, published, status
) VALUES
(1, 1, '春季数学思维挑战赛', '学科竞赛', 'teal', '城南校区', '城南路88号多功能厅',
 DATE_ADD(CURDATE(), INTERVAL 14 DAY), '09:00:00', '12:00:00', DATE_ADD(CURDATE(), INTERVAL 7 DAY),
 60, 0.00, '小学3-4年级',
 '面向学趣学员的春季数学思维挑战，注重解题策略与数感表达。',
 '["现场讲解","奖牌证书","家长观摩"]', NULL, NULL, 1, 'ACTIVE'),
(2, 1, '趣味积木创意赛', '趣味赛事', 'orange', '城南校区', '城南路88号创客教室',
 DATE_ADD(CURDATE(), INTERVAL 21 DAY), '14:00:00', '16:30:00', DATE_ADD(CURDATE(), INTERVAL 14 DAY),
 40, 49.00, '小学1-3年级',
 '用积木完成主题搭建，锻炼空间想象与团队协作。',
 '主题搭建,团队协作,作品展示', NULL, NULL, 1, 'ACTIVE'),
(3, 1, '硬笔书法作品赛', '作品赛', 'indigo', '总部', '中心大道1号展厅',
 DATE_ADD(CURDATE(), INTERVAL 30 DAY), '10:00:00', '11:30:00', DATE_ADD(CURDATE(), INTERVAL 20 DAY),
 50, 0.00, '小学1-3年级',
 '提交硬笔书法作品参评，优秀作品入选学期展。',
 '["作品装裱","专家点评"]', NULL, NULL, 1, 'ACTIVE'),
(4, 1, '寒假编程主题周', '主题赛事', 'violet', '城南校区', '城南路88号机房',
 DATE_SUB(CURDATE(), INTERVAL 45 DAY), '09:30:00', '16:00:00', DATE_SUB(CURDATE(), INTERVAL 50 DAY),
 30, 199.00, '小学3-6年级',
 '寒假主题编程挑战周，完成闯关任务可获徽章。',
 '闯关任务,徽章奖励', NULL, '圆满收官，共 28 名学员完成全部闯关。', 1, 'ACTIVE'),
(5, 1, '秋季口算达人赛', '学科竞赛', 'teal', '城南校区', '城南路88号A203',
 DATE_SUB(CURDATE(), INTERVAL 10 DAY), '15:00:00', '16:30:00', DATE_SUB(CURDATE(), INTERVAL 15 DAY),
 80, 0.00, '小学1-4年级',
 '限时口算挑战，按年级分组计时排名。',
 '["计时排名","年级分组"]', NULL, '一诺同学获得三年级组冠军。', 1, 'ACTIVE');

-- 启航英语 org=2：2 场
INSERT INTO activities (
    id, org_id, title, category, cover_tone, campus, address,
    start_date, start_time, end_time, enroll_deadline,
    capacity, fee, target_grade, summary, highlights, gallery, recap, published, status
) VALUES
(6, 2, '少儿英语口语秀', '口语赛事', 'sky', '河东校区', '河东大道66号报告厅',
 DATE_ADD(CURDATE(), INTERVAL 10 DAY), '18:30:00', '20:00:00', DATE_ADD(CURDATE(), INTERVAL 5 DAY),
 48, 0.00, '小学2-4年级',
 '英文短剧与演讲展示，锻炼口语表达与舞台自信。',
 '["短剧表演","即兴问答"]', NULL, NULL, 1, 'ACTIVE'),
(7, 2, '自然拼读闯关营回顾', '趣味赛事', 'lime', '河东校区', '河东大道66号E201',
 DATE_SUB(CURDATE(), INTERVAL 20 DAY), '10:00:00', '12:00:00', DATE_SUB(CURDATE(), INTERVAL 25 DAY),
 36, 99.00, '小学1-2年级',
 '拼读闯关营结营展示，回顾本学期学习成果。',
 '拼读闯关,结营证书', NULL, '结营展示顺利完成。', 1, 'ACTIVE');
