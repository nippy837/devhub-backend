-- 上线前手动执行；不修改现有 accounts 表及数据。
CREATE TABLE IF NOT EXISTS app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(24) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS snake_games (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    seed BIGINT NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    started_at BIGINT NOT NULL,
    score INT NULL,
    outcome VARCHAR(10) NULL,
    finished_at BIGINT NULL,
    INDEX idx_snake_user_score (user_id, score),
    INDEX idx_snake_user_started (user_id, started_at),
    FOREIGN KEY (user_id) REFERENCES app_users(id)
);
