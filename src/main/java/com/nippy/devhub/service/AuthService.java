package com.nippy.devhub.service;

import com.nippy.devhub.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Service
public class AuthService {
    private static final String USER_ID = "devhub.userId";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final String dummyHash;

    public AuthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.dummyHash = hash("dummy-password-for-constant-work");
    }

    public Map<String, Object> register(String username, String password, HttpServletRequest request) {
        String name = username.toLowerCase(java.util.Locale.ROOT);
        try {
            jdbc.update("INSERT INTO app_users (username, password_hash) VALUES (?, ?)", name, hash(password));
        } catch (DuplicateKeyException e) {
            throw new ApiException(409, "用户名已被使用");
        }
        Long id = jdbc.queryForObject("SELECT id FROM app_users WHERE username = ?", Long.class, name);
        signIn(request, id);
        return current(request);
    }

    public Map<String, Object> login(String username, String password, HttpServletRequest request) {
        var rows = jdbc.queryForList("SELECT id, password_hash FROM app_users WHERE username = ?",
                username.toLowerCase(java.util.Locale.ROOT));
        String stored = rows.isEmpty() ? dummyHash : (String) rows.getFirst().get("password_hash");
        boolean valid = verify(password, stored);
        if (rows.isEmpty() || !valid) throw new ApiException(401, "用户名或密码不正确");
        signIn(request, ((Number) rows.getFirst().get("id")).longValue());
        return current(request);
    }

    private void signIn(HttpServletRequest request, Long id) {
        HttpSession previous = request.getSession(false);
        if (previous != null) previous.invalidate();
        request.getSession(true).setAttribute(USER_ID, id);
    }

    public Long userId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Long) session.getAttribute(USER_ID);
    }

    public Long requireUser(HttpServletRequest request) {
        Long id = userId(request);
        if (id == null) throw new ApiException(401, "请先登录");
        return id;
    }

    public Map<String, Object> current(HttpServletRequest request) {
        Long id = userId(request);
        if (id == null) return null;
        var rows = jdbc.queryForList("SELECT id, username FROM app_users WHERE id = ?", id);
        if (rows.isEmpty()) {
            request.getSession().invalidate();
            return null;
        }
        return rows.getFirst();
    }

    private static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "600000:" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(derive(password, salt, 600000));
    }

    private static boolean verify(String password, String stored) {
        String[] parts = stored.split(":");
        return MessageDigest.isEqual(Base64.getDecoder().decode(parts[2]),
                derive(password, Base64.getDecoder().decode(parts[1]), Integer.parseInt(parts[0])));
    }

    private static byte[] derive(String password, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        } finally { spec.clearPassword(); }
    }
}
