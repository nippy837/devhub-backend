package com.nippy.devhub.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface AuthService {
    Map<String, Object> register(String username, String password, HttpServletRequest request);

    Map<String, Object> login(String username, String password, HttpServletRequest request);

    Long userId(HttpServletRequest request);

    Long requireUser(HttpServletRequest request);

    Map<String, Object> current(HttpServletRequest request);
}
