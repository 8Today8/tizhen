package com.tizhen.core.account.model;

import java.time.Instant;

/**
 * Redis 中保存的后台会话记录。空闲超时由 Redis TTL 控制，绝对超时依赖 createdAt 判断。
 */
public record AdminSessionRecord(Long adminId, Instant createdAt) {
}
