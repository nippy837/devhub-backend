package com.nippy.devhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nippy.devhub.common.ApiException;
import com.nippy.devhub.dto.AiChatDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;

@Service
public class AiChatService {
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;
    private final URI endpoint;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final Semaphore capacity = new Semaphore(4);
    private final Set<Long> activeUsers = new HashSet<>();
    private final Map<Long, long[]> attempts = new HashMap<>();

    public AiChatService(ObjectMapper mapper,
                         @Value("${ai.api-key:}") String apiKey,
                         @Value("${ai.model:}") String model,
                         @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.mapper = mapper;
        this.apiKey = apiKey.trim();
        this.model = model.trim();
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/responses");
        boolean local = Set.of("localhost", "127.0.0.1", "[::1]").contains(Objects.toString(endpoint.getHost(), ""));
        if (endpoint.getHost() == null || endpoint.getUserInfo() != null || endpoint.getQuery() != null
                || endpoint.getFragment() != null || !("https".equals(endpoint.getScheme())
                || (local && "http".equals(endpoint.getScheme()))))
            throw new IllegalArgumentException("OPENAI_BASE_URL 必须是 HTTPS API 基础地址（本机调试可用 HTTP）");
    }

    public Map<String, Object> status() {
        return Map.of("configured", !apiKey.isEmpty() && !model.isEmpty(), "model", model);
    }

    public Map<String, Object> chat(Long userId, AiChatDTO body) {
        if (apiKey.isEmpty() || model.isEmpty()) throw new ApiException(503, "AI 服务尚未配置，请联系管理员设置 API Key 和模型");
        var messages = body.getMessages();
        int total = 0;
        for (int i = 0; i < messages.size(); i++) {
            var message = messages.get(i);
            if (!(i % 2 == 0 ? "user" : "assistant").equals(message.getRole()))
                throw new ApiException(400, "消息必须从用户开始，按用户和助手交替发送");
            total += message.getContent().length();
        }
        if (messages.size() % 2 == 0) throw new ApiException(400, "最后一条必须是用户消息");
        if (total > 60000) throw new ApiException(400, "对话内容过长，请新建对话");
        acquire(userId);
        try {
            var payload = Map.of("model", model, "input", messages,
                    "instructions", "你是 DevHub 的 AI 助手。默认使用中文，清晰准确地回答用户的问题。你只能看到用户发送的对话，不能访问项目文件、账号资料或执行操作。",
                    "store", false, "max_output_tokens", 2048);
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload))).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            // 上游错误正文可能包含凭据或网关信息，不原样返回，也不记录对话。
            if (code == 429) throw new ApiException(429, "AI 服务额度不足或请求过于频繁，请稍后重试或联系管理员");
            if (code == 401 || code == 403) throw new ApiException(502, "AI 服务认证失败，请联系管理员检查 API Key 和模型权限");
            if (code < 200 || code >= 300) throw new ApiException(502, "AI 服务请求失败，请稍后重试或联系管理员检查接口和模型配置");
            JsonNode data = mapper.readTree(response.body());
            if (data == null || !data.path("error").isNull() && !data.path("error").isMissingNode())
                throw new ApiException(502, "AI 服务未能完成回答，请重试");
            StringBuilder answer = new StringBuilder();
            for (JsonNode item : data.path("output")) {
                if (!"message".equals(item.path("type").asText())) continue;
                for (JsonNode part : item.path("content")) {
                    String type = part.path("type").asText();
                    String value = "output_text".equals(type) ? part.path("text").asText("")
                            : "refusal".equals(type) ? part.path("refusal").asText("") : "";
                    if (!value.isBlank()) {
                        if (!answer.isEmpty()) answer.append("\n\n");
                        answer.append(value);
                    }
                }
            }
            if (answer.isEmpty()) throw new ApiException(502, "AI 未返回文本，请缩短问题后重试");
            return Map.of("content", answer.toString(), "model", model,
                    "truncated", "incomplete".equals(data.path("status").asText()));
        } catch (HttpTimeoutException e) {
            throw new ApiException(504, "AI 回答超时，请稍后重试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(503, "AI 请求已中断，请重试");
        } catch (IOException e) {
            throw new ApiException(502, "无法连接 AI 服务或响应格式无效，请联系管理员检查配置");
        } finally {
            synchronized (this) { activeUsers.remove(userId); }
            capacity.release();
        }
    }

    private synchronized void acquire(Long userId) {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> now - entry.getValue()[0] >= 60000);
        if (activeUsers.contains(userId)) throw new ApiException(429, "上一条问题仍在处理中，请稍后再试");
        if (!attempts.containsKey(userId) && attempts.size() >= 10000) throw new ApiException(429, "服务繁忙，请稍后重试");
        long[] window = attempts.computeIfAbsent(userId, key -> new long[]{now, 0});
        if (window[1] >= 10) throw new ApiException(429, "每分钟最多发送 10 条问题，请稍后重试");
        if (!capacity.tryAcquire()) throw new ApiException(429, "AI 服务繁忙，请稍后重试");
        window[1]++;
        activeUsers.add(userId);
    }
}
