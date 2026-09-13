package com.nippy.devhub.service.impl;

import com.nippy.devhub.service.SnakeService;

import com.nippy.devhub.common.ApiException;
import com.nippy.devhub.service.SnakeRules;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SnakeServiceImpl implements SnakeService {
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();
    // 仅用于兼容更新前尚未结束的对局；新对局只允许 normal。
    private static final Map<String, Integer> SPEEDS = Map.of("easy", 200, "normal", 140, "hard", 90);

    public SnakeServiceImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    @Override
    public Map<String, Object> start(long userId, String difficulty) {
        if (!"normal".equals(difficulty)) throw new ApiException(400, "贪吃蛇统一使用中速，请刷新页面");
        jdbc.queryForObject("SELECT id FROM app_users WHERE id = ? FOR UPDATE", Long.class, userId);
        long now = System.currentTimeMillis();
        Integer recent = jdbc.queryForObject("SELECT COUNT(*) FROM snake_games WHERE user_id = ? AND started_at > ?",
                Integer.class, userId, now - 60000);
        if (recent != null && recent >= 20) throw new ApiException(429, "开始游戏过于频繁，请稍后再试");
        String id = UUID.randomUUID().toString();
        long seed = Integer.toUnsignedLong(random.nextInt());
        if (seed == 0) seed = 1;
        jdbc.update("INSERT INTO snake_games (id, user_id, seed, difficulty, started_at) VALUES (?, ?, ?, ?, ?)",
                id, userId, seed, difficulty, now);
        return Map.of("id", id, "seed", seed);
    }

    @Transactional
    @Override
    public Map<String, Object> finish(long userId, String id, String moves) {
        var rows = jdbc.queryForList("SELECT seed, difficulty, started_at, score, outcome FROM snake_games WHERE id = ? AND user_id = ? FOR UPDATE", id, userId);
        if (rows.isEmpty()) throw new ApiException(404, "对局不存在");
        var row = rows.getFirst();
        // 重试返回同一成绩，防止重复提交重复计分。
        if (row.get("score") != null) return Map.of("score", row.get("score"), "outcome", row.get("outcome"));
        long elapsed = System.currentTimeMillis() - ((Number) row.get("started_at")).longValue();
        long expected = (long) moves.length() * SPEEDS.get((String) row.get("difficulty"));
        if (elapsed + 1000 < expected) throw new ApiException(400, "对局时长与操作记录不匹配");
        SnakeRules result = SnakeRules.replay(((Number) row.get("seed")).longValue(), moves);
        jdbc.update("UPDATE snake_games SET score = ?, outcome = ?, finished_at = ? WHERE id = ?",
                result.getScore(), result.getOutcome(), System.currentTimeMillis(), id);
        return Map.of("score", result.getScore(), "outcome", result.getOutcome());
    }

    @Override
    public Map<String, Object> leaderboard(Long userId) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT u.id, u.username, MAX(g.score) AS best_score
                FROM app_users u JOIN snake_games g ON u.id = g.user_id
                WHERE g.score > 0 AND g.difficulty = 'normal' GROUP BY u.id, u.username
                ORDER BY best_score DESC, u.id ASC LIMIT 50
                """, (rs, index) -> Map.of("rank", index + 1, "userId", rs.getLong("id"),
                "username", rs.getString("username"), "score", rs.getInt("best_score")));
        Integer best = userId == null ? 0 : jdbc.queryForObject(
                "SELECT COALESCE(MAX(score), 0) FROM snake_games WHERE user_id = ? AND difficulty = 'normal'", Integer.class, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entries", rows);
        result.put("myBest", best);
        return result;
    }
}
