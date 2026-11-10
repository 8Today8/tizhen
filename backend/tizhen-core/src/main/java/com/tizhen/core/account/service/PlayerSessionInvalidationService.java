package com.tizhen.core.account.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 选手会话失效。Spring Session Redis 通过 principalName 索引会话；
 * 账号被禁用时删除索引及其会话键，已登录请求随即失效。
 */
@Service
public class PlayerSessionInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(PlayerSessionInvalidationService.class);
    private static final String PRINCIPAL_INDEX_NAME =
            "org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME";

    private final StringRedisTemplate redisTemplate;
    private final String namespace;

    public PlayerSessionInvalidationService(
            StringRedisTemplate redisTemplate,
            @Value("${spring.session.redis.namespace:tizhen:session}") String namespace) {
        this.redisTemplate = redisTemplate;
        this.namespace = namespace;
    }

    public void invalidate(Long userId, String username) {
        invalidatePrincipal(String.valueOf(userId));
        invalidatePrincipal(username);
    }

    private void invalidatePrincipal(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            return;
        }
        String indexKey = namespace + ":index:" + PRINCIPAL_INDEX_NAME + ":" + principalName;
        Set<String> sessionIds = redisTemplate.opsForSet().members(indexKey);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                redisTemplate.delete(namespace + ":sessions:" + sessionId);
                redisTemplate.delete(namespace + ":sessions:expires:" + sessionId);
            }
        }
        redisTemplate.delete(indexKey);
        log.debug("选手会话失效 principal={}, sessions={}", principalName,
                sessionIds == null ? 0 : sessionIds.size());
    }
}
