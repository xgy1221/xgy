package com.xgy.cloud.service;

import com.xgy.cloud.common.BizException;
import com.xgy.cloud.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties securityProperties;

    public void check(String phone, String ip) {
        int maxPhone = Math.max(1, securityProperties.getLoginMaxPerPhone());
        int maxIp = Math.max(1, securityProperties.getLoginMaxPerIp());
        int window = Math.max(30, securityProperties.getLoginWindowSeconds());

        bump("xgy:rl:login:phone:" + phone, maxPhone, window, "该手机号登录过于频繁，请稍后再试");
        if (ip != null && !ip.isBlank()) {
            bump("xgy:rl:login:ip:" + ip, maxIp, window, "当前网络登录过于频繁，请稍后再试");
        }
    }

    private void bump(String key, int max, int windowSeconds, String message) {
        Long n = redisTemplate.opsForValue().increment(key);
        if (n != null && n == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        if (n != null && n > max) {
            throw new BizException(429, message);
        }
    }
}
