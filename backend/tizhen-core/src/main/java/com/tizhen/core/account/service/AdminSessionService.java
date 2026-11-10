package com.tizhen.core.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.core.account.model.AdminSessionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * 后台独立会话存储。使用单独的 Redis Key 与 Cookie，与选手会话互不覆盖。
 */
@Service
public class AdminSessionService {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionService.class);
    private static final String SESSION_KEY_PREFIX = "tizhen:admin:session:";
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration idleTimeout;
    private final Duration absoluteTimeout;

    public AdminSessionService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${tizhen.security.admin.session.idle-timeout-seconds:1800}") long idleTimeoutSeconds,
            @Value("${tizhen.security.admin.session.absolute-timeout-seconds:28800}") long absoluteTimeoutSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.idleTimeout = Duration.ofSeconds(idleTimeoutSeconds);
        this.absoluteTimeout = Duration.ofSeconds(absoluteTimeoutSeconds);
    }

    public Duration getIdleTimeout() {
        return idleTimeout;
    }

    public String create(Long adminId) {
        String token = generateToken();
        AdminSessionRecord record = new AdminSessionRecord(adminId, Instant.now());
        redisTemplate.opsForValue().set(key(token), write(record), idleTimeout);
        return token;
    }

    /**
     * 校验令牌并续期空闲时间。令牌不存在、格式错误或超过绝对超时均返回空。
     */
    public Optional<Long> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String key = key(token);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }

        AdminSessionRecord record;
        try {
            record = objectMapper.readValue(value, AdminSessionRecord.class);
        } catch (JsonProcessingException exception) {
            log.warn("后台会话格式异常，已清理 token={}", maskToken(token));
            redisTemplate.delete(key);
            return Optional.empty();
        }

        if (record.createdAt() == null
                || record.createdAt().plus(absoluteTimeout).isBefore(Instant.now())) {
            redisTemplate.delete(key);
            return Optional.empty();
        }

        redisTemplate.expire(key, idleTimeout);
        return Optional.of(record.adminId());
    }

    public void delete(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete(key(token));
        }
    }

    /**
     * 管理员被禁用或重置密码时，删除其全部后台会话。
     */
    public void deleteByAdminId(Long adminId) {
        if (adminId == null) {
            return;
        }
        ScanOptions options = ScanOptions.scanOptions()
                .match(SESSION_KEY_PREFIX + "*")
                .count(200)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String sessionKey = cursor.next();
                String value = redisTemplate.opsForValue().get(sessionKey);
                if (value == null) {
                    continue;
                }
                try {
                    AdminSessionRecord record = objectMapper.readValue(value, AdminSessionRecord.class);
                    if (adminId.equals(record.adminId())) {
                        redisTemplate.delete(sessionKey);
                    }
                } catch (JsonProcessingException exception) {
                    redisTemplate.delete(sessionKey);
                }
            }
        }
    }

    private String key(String token) {
        return SESSION_KEY_PREFIX + token;
    }

    private String write(AdminSessionRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("后台会话序列化失败", exception);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String maskToken(String token) {
        return token.length() <= 8 ? "***" : token.substring(0, 8) + "***";
    }
}
