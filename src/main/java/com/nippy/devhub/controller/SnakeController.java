package com.nippy.devhub.controller;

import com.nippy.devhub.common.Result;
import com.nippy.devhub.service.AuthService;
import com.nippy.devhub.service.SnakeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/snake")
@RequiredArgsConstructor
public class SnakeController {
    private final AuthService auth;
    private final SnakeService snake;

    @Data
    public static class StartRequest {
        @NotNull @Pattern(regexp = "normal", message = "贪吃蛇统一使用中速，请刷新页面")
        private String difficulty = "normal";
    }
    @Data
    public static class FinishRequest {
        @NotNull @Size(max = 50000) @Pattern(regexp = "[UDLR]*", message = "方向记录无效")
        private String moves;
    }

    @PostMapping("/games")
    public Result<Map<String, Object>> start(@Valid @RequestBody StartRequest body, HttpServletRequest request) {
        return Result.success(snake.start(auth.requireUser(request), body.difficulty));
    }
    @PostMapping("/games/{id}/finish")
    public Result<Map<String, Object>> finish(@PathVariable String id, @Valid @RequestBody FinishRequest body, HttpServletRequest request) {
        return Result.success(snake.finish(auth.requireUser(request), id, body.moves));
    }
    @GetMapping("/leaderboard")
    public Result<Map<String, Object>> leaderboard(HttpServletRequest request) {
        return Result.success(snake.leaderboard(auth.userId(request)));
    }
}
