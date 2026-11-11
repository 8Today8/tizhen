package com.tizhen.core.judge.service;

import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 选手提交频率限制。按用户计数，窗口外自动过期，不阻塞合法作答。
 */
@Service
public class SubmissionRateLimiter {

    private static final String KEY_PREFIX = "tizhen:judge:submit:";

    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final Duration window;

    public SubmissionRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${tizhen.submission.rate-limit.max-requests:10}") int maxRequests,
            @Value("${tizhen.submission.rate-limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public void assertAllowed(Long userId) {
        String key = KEY_PREFIX + userId;
        Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redisTemplate.expire(key, window);
        }
        if (current != null && current > maxRequests) {
            throw new BusinessException(ErrorCode.SUBMISSION_RATE_LIMITED);
        }
    }
}
