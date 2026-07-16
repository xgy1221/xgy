-- 关键操作审计（登录失败、授权、消课、订单等）
CREATE TABLE IF NOT EXISTS audit_logs (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_id        BIGINT       NULL,
    user_id       BIGINT       NULL,
    phone         VARCHAR(20)  NULL,
    action        VARCHAR(64)  NOT NULL,
    target_type   VARCHAR(64)  NULL,
    target_id     VARCHAR(64)  NULL,
    detail        VARCHAR(1024) NULL,
    ip            VARCHAR(64)  NULL,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_audit_org_time (org_id, created_at),
    INDEX idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
