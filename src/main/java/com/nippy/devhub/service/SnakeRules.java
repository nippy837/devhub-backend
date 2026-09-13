package com.nippy.devhub.service;

import com.nippy.devhub.common.ApiException;
import java.util.ArrayList;
import java.util.List;

/** 服务端复算客户端方向记录，不接受客户端分数。规则与前端 snakeGame.js 保持一致。 */
public final class SnakeRules {
    public static final int MAX_TICKS = 50000;
    private int randomState;
    private final List<Integer> snake = new ArrayList<>(List.of(207, 206, 205));
    private final List<Integer> foods = new ArrayList<>();
    private char direction = 'R';
    private int score;
    private String outcome = "ended";

    private SnakeRules(long seed) {
        randomState = (int) seed;
        if (randomState == 0) randomState = 1;
        refill();
    }

    public static SnakeRules replay(long seed, String moves) {
        if (moves == null || moves.length() > MAX_TICKS) throw new ApiException(400, "对局操作记录过长");
        SnakeRules game = new SnakeRules(seed);
        for (int i = 0; i < moves.length(); i++) {
            if (!game.outcome.equals("ended")) throw new ApiException(400, "对局结束后不能继续移动");
            game.step(moves.charAt(i));
        }
        return game;
    }

    public int getScore() { return score; }
    public String getOutcome() { return outcome; }

    private void step(char next) {
        if ("UDLR".indexOf(next) < 0) throw new ApiException(400, "方向记录无效");
        if ((direction == 'U' && next == 'D') || (direction == 'D' && next == 'U')
                || (direction == 'L' && next == 'R') || (direction == 'R' && next == 'L'))
            throw new ApiException(400, "小蛇不能直接反向移动");
        direction = next;
        int x = snake.getFirst() % 20;
        int y = snake.getFirst() / 20;
        x = (x + (next == 'R' ? 1 : next == 'L' ? -1 : 0) + 20) % 20;
        y = (y + (next == 'D' ? 1 : next == 'U' ? -1 : 0) + 20) % 20;
        int head = y * 20 + x;
        boolean eating = foods.contains(head);
        int occupied = snake.size() - (eating ? 0 : 1);
        if (snake.subList(0, occupied).contains(head)) { outcome = "over"; return; }
        snake.addFirst(head);
        if (eating) {
            foods.remove(Integer.valueOf(head));
            score += 20;
            refill();
        } else snake.removeLast();
        if (snake.size() == 400) outcome = "won";
    }

    private void refill() {
        while (foods.size() < 8) {
            boolean[] occupied = new boolean[400];
            for (int cell : snake) occupied[cell] = true;
            for (int cell : foods) occupied[cell] = true;
            List<Integer> free = new ArrayList<>();
            for (int cell = 0; cell < 400; cell++) if (!occupied[cell]) free.add(cell);
            if (free.isEmpty()) return;
            randomState ^= randomState << 13;
            randomState ^= randomState >>> 17;
            randomState ^= randomState << 5;
            int index = (int) ((Integer.toUnsignedLong(randomState) / 4294967296.0) * free.size());
            foods.add(free.get(index));
        }
    }
}
