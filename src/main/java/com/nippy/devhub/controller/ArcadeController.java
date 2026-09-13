package com.nippy.devhub.controller;

import com.nippy.devhub.common.Result;
import com.nippy.devhub.service.ArcadeService;
import com.nippy.devhub.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arcade/{kind}")
@RequiredArgsConstructor
public class ArcadeController {
    private final AuthService auth;
    private final ArcadeService arcade;
    @Data
    public static class StartRequest {
        @NotBlank @Size(max = 16) private String variant;
    }
    @Data
    public static class FinishRequest {
        @NotNull @Size(max = 20000) private List<@NotBlank @Size(max = 6) String> actions;
    }
    @PostMapping("/games")
    public Result<Map<String, Object>> start(@PathVariable String kind, @Valid @RequestBody StartRequest body, HttpServletRequest request) {
        return Result.success(arcade.start(auth.requireUser(request), kind, body.variant));
    }
    @PostMapping("/games/{id}/finish")
    public Result<Map<String, Object>> finish(@PathVariable String kind, @PathVariable String id, @Valid @RequestBody FinishRequest body, HttpServletRequest request) {
        return Result.success(arcade.finish(auth.requireUser(request), kind, id, body.actions));
    }
    @GetMapping("/leaderboard")
    public Result<Map<String, Object>> leaderboard(@PathVariable String kind, @RequestParam String variant, HttpServletRequest request) {
        return Result.success(arcade.leaderboard(auth.userId(request), kind, variant));
    }
}
