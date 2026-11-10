package com.tizhen.core.account.model;

import java.time.Instant;

/**
 * 管理员账号视图，不包含密码哈希。
 */
public record AdminAccountView(
        String id,
        String username,
        String displayName,
        String avatarUrl,
        String roleCode,
        String roleName,
        String status,
        Instant lastLoginAt,
        Instant createdAt
) {
}
