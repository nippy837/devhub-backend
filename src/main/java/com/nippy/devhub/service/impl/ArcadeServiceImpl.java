package com.nippy.devhub.service.impl;

import com.nippy.devhub.service.ArcadeService;

import com.nippy.devhub.common.ApiException;
import com.nippy.devhub.service.ArcadeRules;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.*;

@Service
public class ArcadeServiceImpl implements ArcadeService {
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();
    public ArcadeServiceImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    @Override
    public Map<String, Object> start(long userId, String kind, String variant) {
        ArcadeRules.validate(kind, variant);
        jdbc.queryForObject("SELECT id FROM app_users WHERE id = ? FOR UPDATE", Long.class, userId);
        long now = System.currentTimeMillis();
        Integer recent = jdbc.queryForObject("SELECT COUNT(*) FROM arcade_games WHERE user_id = ? AND started_at > ?", Integer.class, userId, now - 60000);
        if (recent != null && recent >= 30) throw new ApiException(429, "开始游戏过于频繁，请稍后重试");
        String id = UUID.randomUUID().toString();
        long seed = Integer.toUnsignedLong(random.nextInt());
        if (seed == 0) seed = 1;
        jdbc.update("INSERT INTO arcade_games (id, user_id, game_kind, variant, seed, started_at) VALUES (?, ?, ?, ?, ?, ?)", id, userId, kind, variant, seed, now);
        return Map.of("id", id, "seed", seed);
    }

    @Transactional
    @Override
    public Map<String, Object> finish(long userId, String kind, String id, List<String> actions) {
        var rows = jdbc.queryForList("SELECT variant, seed, started_at, metric, outcome FROM arcade_games WHERE id = ? AND user_id = ? AND game_kind = ? FOR UPDATE", id, userId, kind);
        if (rows.isEmpty()) throw new ApiException(404, "对局不存在");
        var row = rows.getFirst();
        if (row.get("metric") != null) return Map.of("score", row.get("metric"), "outcome", row.get("outcome"));
        ArcadeRules replay = ArcadeRules.replay(kind, (String) row.get("variant"), ((Number) row.get("seed")).longValue(), actions);
        long elapsed = Math.max(1, System.currentTimeMillis() - ((Number) row.get("started_at")).longValue());
        long metric = kind.equals("2048") ? replay.getScore() : !replay.getOutcome().equals("won") ? 0 : kind.equals("sokoban") ? replay.getSteps() : elapsed;
        String outcome = replay.getOutcome().equals("running") ? "ended" : replay.getOutcome();
        jdbc.update("UPDATE arcade_games SET metric = ?, outcome = ?, finished_at = ? WHERE id = ?", metric, outcome, System.currentTimeMillis(), id);
        return Map.of("score", metric, "outcome", outcome);
    }

    @Override
    public Map<String, Object> leaderboard(Long userId, String kind, String variant) {
        if ("sokoban".equals(kind)) {
            // 兼容旧客户端的关卡参数，但始终返回累计通关排行榜。
            if (variant != null) ArcadeRules.validate(kind, variant);
            return sokobanLeaderboard(userId);
        }
        if (variant == null) throw new ApiException(400, "请指定游戏难度");
        ArcadeRules.validate(kind, variant);
        // 聚合方向只由服务端的游戏类型决定，用户输入仍使用绑定参数。
        String aggregate = kind.equals("2048") ? "MAX" : "MIN";
        String order = kind.equals("2048") ? "DESC" : "ASC";
        var entries = jdbc.query("SELECT u.id, u.username, " + aggregate + "(g.metric) AS best_metric FROM app_users u JOIN arcade_games g ON u.id = g.user_id "
                + "WHERE g.game_kind = ? AND g.variant = ? AND g.metric > 0 GROUP BY u.id, u.username ORDER BY best_metric " + order + ", u.id ASC LIMIT 50",
                (rs, index) -> Map.of("rank", index + 1, "userId", rs.getLong("id"), "username", rs.getString("username"), "score", rs.getLong("best_metric")), kind, variant);
        Long best = userId == null ? null : jdbc.queryForObject("SELECT " + aggregate + "(metric) FROM arcade_games WHERE user_id = ? AND game_kind = ? AND variant = ? AND metric > 0", Long.class, userId, kind, variant);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entries", entries); result.put("myBest", best);
        return result;
    }

    private Map<String, Object> sokobanLeaderboard(Long userId) {
        var entries = jdbc.query("""
                SELECT u.id, u.username, COUNT(DISTINCT g.variant) AS cleared_levels
                FROM app_users u JOIN arcade_games g ON u.id = g.user_id
                WHERE g.game_kind = 'sokoban' AND g.outcome = 'won'
                GROUP BY u.id, u.username
                ORDER BY cleared_levels DESC, u.id ASC LIMIT 50
                """, (rs, index) -> Map.of("rank", index + 1, "userId", rs.getLong("id"),
                "username", rs.getString("username"), "score", rs.getLong("cleared_levels")));
        Long cleared = userId == null ? null : jdbc.queryForObject("""
                SELECT COUNT(DISTINCT variant) FROM arcade_games
                WHERE user_id = ? AND game_kind = 'sokoban' AND outcome = 'won'
                """, Long.class, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entries", entries);
        result.put("myBest", cleared);
        return result;
    }
}
