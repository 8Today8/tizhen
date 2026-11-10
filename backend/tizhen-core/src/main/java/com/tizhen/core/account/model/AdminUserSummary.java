package com.tizhen.core.account.model;

import java.time.Instant;

/**
 * 后台用户列表/详情基本资料。
 */
public record AdminUserSummary(
        String id,
        String username,
        String nickname,
        String email,
        String phone,
        String avatarUrl,
        String school,
        String cfHandle,
        String status,
        Instant lastLoginAt,
        Instant createdAt
) {
}
