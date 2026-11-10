package com.tizhen.core.account.service;

import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

/**
 * 管理员登录失败限流：用户名 + IP 维度，窗口外自动过期。
 */
@Service
public class AdminLoginRateLimiter {

    private static final String FAIL_KEY_PREFIX = "tizhen:auth:admin:fail:";
    private static final String LOCK_KEY_PREFIX = "tizhen:auth:admin:lock:";

    private final StringRedisTemplate redisTemplate;
    private final int maxFailures;
    private final Duration window;
    private final Duration lockDuration;

    public AdminLoginRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${tizhen.security.admin.login.max-failures:5}") int maxFailures,
            @Value("${tizhen.security.admin.login.window-seconds:900}") long windowSeconds,
            @Value("${tizhen.security.admin.login.lock-seconds:900}") long lockSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxFailures = maxFailures;
        this.window = Duration.ofSeconds(windowSeconds);
        this.lockDuration = Duration.ofSeconds(lockSeconds);
    }

    public void assertNotLocked(String username, String ipAddress) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(username, ipAddress)))) {
            throw new BusinessException(ErrorCode.COMMON_RATE_LIMITED, "登录失败次数过多，请稍后重试");
        }
    }

    /**
     * @return 本次失败后是否已触发锁定
     */
    public boolean recordFailure(String username, String ipAddress) {
        String failureKey = failKey(username, ipAddress);
        Long failures = redisTemplate.opsForValue().increment(failureKey);
        if (failures != null && failures == 1L) {
            redisTemplate.expire(failureKey, window);
        }
        if (failures != null && failures >= maxFailures) {
            redisTemplate.opsForValue().set(lockKey(username, ipAddress), "1", lockDuration);
            return true;
        }
        return false;
    }

    public void reset(String username, String ipAddress) {
        redisTemplate.delete(failKey(username, ipAddress));
        redisTemplate.delete(lockKey(username, ipAddress));
    }

    private String failKey(String username, String ipAddress) {
        return FAIL_KEY_PREFIX + identity(username, ipAddress);
    }

    private String lockKey(String username, String ipAddress) {
        return LOCK_KEY_PREFIX + identity(username, ipAddress);
    }

    private String identity(String username, String ipAddress) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return normalizedUsername + ":" + (ipAddress == null ? "" : ipAddress);
    }
}
