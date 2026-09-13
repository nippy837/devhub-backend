package com.nippy.devhub.controller;

import com.nippy.devhub.common.Result;
import com.nippy.devhub.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @Data
    public static class Credentials {
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,24}", message = "用户名需为 3–24 位字母、数字或下划线")
        private String username;
        @NotBlank @Size(min = 8, max = 128, message = "密码需为 8–128 位")
        private String password;
    }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public Result<Map<String, Object>> register(@Valid @RequestBody Credentials body, HttpServletRequest request) {
        return Result.success(auth.register(body.username, body.password, request));
    }
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody Credentials body, HttpServletRequest request) {
        return Result.success(auth.login(body.username, body.password, request));
    }
    @GetMapping("/me")
    public Result<Map<String, Object>> me(HttpServletRequest request) {
        return Result.success(auth.current(request));
    }
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        return Result.success(null);
    }
}
