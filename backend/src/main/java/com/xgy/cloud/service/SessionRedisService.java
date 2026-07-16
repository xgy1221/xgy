package com.xgy.cloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRedisService {

    private static final String SESSION_KEY = "xgy:session:";
    private static final String LAST_ROLE_KEY = "xgy:lastRole:";
    private static final String LAST_STUDENT_KEY = "xgy:lastStudent:";
    private static final String BLACKLIST_KEY = "xgy:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveSession(Long userId, Map<String, Object> meta, Duration ttl) {
        String key = SESSION_KEY + userId;
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(meta), ttl);
        } catch (JsonProcessingException e) {
            log.warn("serialize session failed: {}", e.getMessage());
        }
    }

    public Map<String, Object> getSession(Long userId) {
        String json = redisTemplate.opsForValue().get(SESSION_KEY + userId);
        if (!StringUtils.hasText(json)) {
            return new HashMap<>();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return map;
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    public void clearSession(Long userId) {
        redisTemplate.delete(SESSION_KEY + userId);
    }

    public void saveLastRole(String phone, String role) {
        if (StringUtils.hasText(phone) && StringUtils.hasText(role)) {
            redisTemplate.opsForValue().set(LAST_ROLE_KEY + phone, role, Duration.ofDays(90));
        }
    }

    public String getLastRole(String phone) {
        return redisTemplate.opsForValue().get(LAST_ROLE_KEY + phone);
    }

    public void saveLastStudent(String phone, Long studentId) {
        if (StringUtils.hasText(phone) && studentId != null) {
            redisTemplate.opsForValue().set(LAST_STUDENT_KEY + phone, String.valueOf(studentId), Duration.ofDays(90));
        }
    }

    public Long getLastStudent(String phone) {
        String v = redisTemplate.opsForValue().get(LAST_STUDENT_KEY + phone);
        if (!StringUtils.hasText(v)) {
            return null;
        }
        try {
            return Long.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void blacklistToken(String token, Instant expireAt) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        long seconds = Math.max(1, Duration.between(Instant.now(), expireAt).getSeconds());
        redisTemplate.opsForValue().set(BLACKLIST_KEY + token, "1", seconds, TimeUnit.SECONDS);
    }

    public boolean isTokenBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        Boolean has = redisTemplate.hasKey(BLACKLIST_KEY + token);
        return Boolean.TRUE.equals(has);
    }
}
