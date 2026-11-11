package com.tizhen.core.datagen.service;

import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 造数据任务提交频率限制，避免随机模式绕过提交限流。
 */
@Service
public class DataGenRateLimiter {

    private static final String KEY_PREFIX = "tizhen:datagen:create:";

    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final Duration window;

    public DataGenRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${tizhen.datagen.rate-limit.max-requests:10}") int maxRequests,
            @Value("${tizhen.datagen.rate-limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = Math.max(maxRequests, 1);
        this.window = Duration.ofSeconds(Math.max(windowSeconds, 1));
    }

    public void assertAllowed(Long userId) {
        String key = KEY_PREFIX + userId;
        Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redisTemplate.expire(key, window);
        }
        if (current != null && current > maxRequests) {
            throw new BusinessException(ErrorCode.COMMON_RATE_LIMITED, "造数据任务提交过于频繁");
        }
    }
}
