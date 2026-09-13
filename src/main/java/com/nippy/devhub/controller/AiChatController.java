package com.nippy.devhub.controller;

import com.nippy.devhub.common.Result;
import com.nippy.devhub.dto.AiChatDTO;
import com.nippy.devhub.service.AiChatService;
import com.nippy.devhub.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    private final AiChatService ai;
    private final AuthService auth;

    @GetMapping("/status")
    public Result<Map<String, Object>> status() { return Result.success(ai.status()); }

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@Valid @RequestBody AiChatDTO body, HttpServletRequest request) {
        return Result.success(ai.chat(auth.requireUser(request), body));
    }
}
