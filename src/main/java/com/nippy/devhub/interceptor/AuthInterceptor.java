package com.nippy.devhub.interceptor;

import com.nippy.devhub.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final Map<String, long[]> attempts = new HashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("Cache-Control", "no-store");
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
            // 自定义头不能由跨站表单发送；不开放 CORS，跨站 fetch 预检也不会获准。
            if (!"1".equals(request.getHeader("X-DevHub-Request")))
                throw new ApiException(403, "请求来源校验失败，请刷新页面重试");
        }
        if (request.getRequestURI().matches("/api/auth/(login|register)")) throttle(request.getRemoteAddr());
        return true;
    }

    private synchronized void throttle(String ip) {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(entry -> now - entry.getValue()[0] > 60000);
        if (!attempts.containsKey(ip) && attempts.size() >= 10000)
            throw new ApiException(429, "请求过于频繁，请稍后重试");
        long[] window = attempts.computeIfAbsent(ip, key -> new long[]{now, 0});
        if (++window[1] > 30) throw new ApiException(429, "尝试次数过多，请一分钟后重试");
    }
}
