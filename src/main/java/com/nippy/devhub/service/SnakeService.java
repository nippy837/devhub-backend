package com.nippy.devhub.service;

import java.util.Map;

public interface SnakeService {
    Map<String, Object> start(long userId, String difficulty);

    Map<String, Object> finish(long userId, String id, String moves);

    Map<String, Object> leaderboard(Long userId);
}
