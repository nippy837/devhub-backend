package com.nippy.devhub.service;

import java.util.List;
import java.util.Map;

public interface ArcadeService {
    Map<String, Object> start(long userId, String kind, String variant);

    Map<String, Object> finish(long userId, String kind, String id, List<String> actions);

    Map<String, Object> leaderboard(Long userId, String kind, String variant);
}
