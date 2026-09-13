-- 手动执行：先确保已执行 20260913_auth_snake.sql，存在 app_users 表。
-- 本脚本仅新增三个小游戏的对局表，不修改现有账号或贪吃蛇记录。
CREATE TABLE IF NOT EXISTS arcade_games (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    game_kind VARCHAR(16) NOT NULL,
    variant VARCHAR(16) NOT NULL,
    seed BIGINT NOT NULL,
    started_at BIGINT NOT NULL,
    metric BIGINT NULL COMMENT '2048分数/推箱子步数/扫雷毫秒；未通关的后两项为0',
    outcome VARCHAR(10) NULL,
    finished_at BIGINT NULL,
    INDEX idx_arcade_rank (game_kind, variant, user_id, metric),
    INDEX idx_arcade_user_started (user_id, started_at),
    FOREIGN KEY (user_id) REFERENCES app_users(id)
);
