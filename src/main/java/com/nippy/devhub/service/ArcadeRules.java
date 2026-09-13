package com.nippy.devhub.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nippy.devhub.common.ApiException;
import lombok.Data;
import lombok.Getter;
import java.io.IOException;
import java.util.*;

/** 三种小游戏的确定性复算规则；不接收客户端分数或通关判定。 */
public final class ArcadeRules {
    public static final int MAX_ACTIONS = 20000;
    private static final List<Level> LEVELS = readLevels();
    private final String kind;
    private int randomState;
    private int width;
    private int height;
    private int[] cells;
    private int player;
    private final Set<Integer> walls = new HashSet<>();
    private final Set<Integer> goals = new HashSet<>();
    private final Set<Integer> boxes = new HashSet<>();
    private final Set<Integer> mines = new HashSet<>();
    private final Set<Integer> flags = new HashSet<>();
    private final Set<Integer> revealed = new HashSet<>();
    private int mineCount;
    @Getter private int score;
    @Getter private int steps;
    @Getter private String outcome = "running";

    @Data
    public static class Level {
        private String id;
        private String name;
        private List<String> map;
    }

    private static List<Level> readLevels() {
        try (var stream = ArcadeRules.class.getResourceAsStream("/sokoban-levels.json")) {
            if (stream == null) throw new IllegalStateException("Missing Sokoban levels");
            return new ObjectMapper().readValue(stream, new TypeReference<>() {});
        } catch (IOException e) { throw new IllegalStateException("Invalid Sokoban levels", e); }
    }

    public static void validate(String kind, String variant) {
        boolean valid = switch (kind) {
            case "2048" -> "classic".equals(variant);
            case "sokoban" -> LEVELS.stream().anyMatch(level -> level.id.equals(variant));
            case "mines" -> Set.of("easy", "medium", "hard").contains(variant);
            default -> false;
        };
        if (!valid) throw new ApiException(400, "游戏或关卡不存在");
    }

    private ArcadeRules(String kind, String variant, long seed) {
        validate(kind, variant);
        this.kind = kind;
        randomState = (int) seed;
        if (randomState == 0) randomState = 1;
        if (kind.equals("2048")) {
            width = height = 4;
            cells = new int[16];
            addTile(); addTile();
        } else if (kind.equals("sokoban")) {
            Level level = LEVELS.stream().filter(item -> item.id.equals(variant)).findFirst().orElseThrow();
            width = level.map.getFirst().length(); height = level.map.size();
            String layout = String.join("", level.map);
            for (int i = 0; i < layout.length(); i++) {
                char tile = layout.charAt(i);
                if (tile == '#') walls.add(i);
                if (".+*".indexOf(tile) >= 0) goals.add(i);
                if ("$*".indexOf(tile) >= 0) boxes.add(i);
                if ("@+".indexOf(tile) >= 0) player = i;
            }
        } else {
            width = height = variant.equals("easy") ? 9 : variant.equals("medium") ? 12 : 16;
            mineCount = variant.equals("easy") ? 10 : variant.equals("medium") ? 24 : 40;
        }
    }

    public static ArcadeRules replay(String kind, String variant, long seed, List<String> actions) {
        if (actions == null || actions.size() > MAX_ACTIONS) throw new ApiException(400, "操作记录过长");
        ArcadeRules game = new ArcadeRules(kind, variant, seed);
        for (String action : actions) {
            if (!game.outcome.equals("running")) throw new ApiException(400, "对局结束后不能继续操作");
            if (action == null || action.length() > 6) throw new ApiException(400, "操作记录无效");
            boolean changed = switch (kind) {
                case "2048" -> game.slide(action);
                case "sokoban" -> game.push(action);
                default -> game.openMine(action);
            };
            if (!changed) throw new ApiException(400, "操作记录包含无效移动");
            game.steps++;
        }
        return game;
    }

    private double random() {
        randomState ^= randomState << 13;
        randomState ^= randomState >>> 17;
        randomState ^= randomState << 5;
        return Integer.toUnsignedLong(randomState) / 4294967296.0;
    }

    private void addTile() {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) if (cells[i] == 0) free.add(i);
        if (!free.isEmpty()) cells[free.get((int) (random() * free.size()))] = random() < 0.9 ? 2 : 4;
    }

    private boolean slide(String action) {
        if (!Set.of("U", "D", "L", "R").contains(action)) return false;
        int[] previous = cells.clone();
        for (int line = 0; line < 4; line++) {
            int[] indices = new int[4];
            List<Integer> values = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                indices[i] = switch (action) {
                    case "L" -> line * 4 + i;
                    case "R" -> line * 4 + 3 - i;
                    case "U" -> i * 4 + line;
                    default -> (3 - i) * 4 + line;
                };
                if (cells[indices[i]] != 0) values.add(cells[indices[i]]);
            }
            List<Integer> merged = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                int value = values.get(i);
                if (i + 1 < values.size() && value == values.get(i + 1)) { value *= 2; score += value; i++; }
                merged.add(value);
            }
            for (int i = 0; i < 4; i++) cells[indices[i]] = i < merged.size() ? merged.get(i) : 0;
        }
        if (Arrays.equals(previous, cells)) return false;
        addTile();
        boolean possible = false;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == 2048) { outcome = "won"; return true; }
            if (cells[i] == 0 || (i % 4 < 3 && cells[i] == cells[i + 1]) || (i < 12 && cells[i] == cells[i + 4])) possible = true;
        }
        if (!possible) outcome = "over";
        return true;
    }

    private int next(int cell, int dx, int dy) {
        int x = cell % width + dx, y = cell / width + dy;
        return x < 0 || x >= width || y < 0 || y >= height ? -1 : y * width + x;
    }

    private boolean push(String action) {
        if (!Set.of("U", "D", "L", "R").contains(action)) return false;
        int dx = action.equals("L") ? -1 : action.equals("R") ? 1 : 0;
        int dy = action.equals("U") ? -1 : action.equals("D") ? 1 : 0;
        int destination = next(player, dx, dy);
        if (destination < 0 || walls.contains(destination)) return false;
        if (boxes.contains(destination)) {
            int beyond = next(destination, dx, dy);
            if (beyond < 0 || walls.contains(beyond) || boxes.contains(beyond)) return false;
            boxes.remove(destination); boxes.add(beyond);
        }
        player = destination;
        if (boxes.containsAll(goals)) outcome = "won";
        return true;
    }

    private List<Integer> neighbors(int index) {
        List<Integer> result = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            if (dx == 0 && dy == 0) continue;
            int cell = next(index, dx, dy);
            if (cell >= 0) result.add(cell);
        }
        return result;
    }

    private boolean openMine(String action) {
        if (!action.matches("[OF][0-9]{1,3}")) return false;
        int index = Integer.parseInt(action.substring(1));
        if (index >= width * height || revealed.contains(index)) return false;
        if (action.charAt(0) == 'F') {
            if (flags.contains(index)) flags.remove(index);
            else { if (flags.size() >= mineCount) return false; flags.add(index); }
            return true;
        }
        if (flags.contains(index)) return false;
        if (mines.isEmpty()) {
            Set<Integer> safe = new HashSet<>(neighbors(index)); safe.add(index);
            List<Integer> candidates = new ArrayList<>();
            for (int i = 0; i < width * height; i++) if (!safe.contains(i)) candidates.add(i);
            while (mines.size() < mineCount) mines.add(candidates.remove((int) (random() * candidates.size())));
        }
        if (mines.contains(index)) { outcome = "over"; return true; }
        ArrayDeque<Integer> queue = new ArrayDeque<>(); queue.add(index);
        while (!queue.isEmpty()) {
            int cell = queue.removeLast();
            if (revealed.contains(cell) || flags.contains(cell) || mines.contains(cell)) continue;
            revealed.add(cell);
            List<Integer> nearby = neighbors(cell);
            if (nearby.stream().noneMatch(mines::contains)) queue.addAll(nearby);
        }
        if (revealed.size() == width * height - mineCount) outcome = "won";
        return true;
    }
}
