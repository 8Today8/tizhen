package com.tizhen.core.account.model;

import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.entity.AppUser;

import java.time.Instant;

/**
 * 选手本人资料。仅用于当前登录用户，不包含密码哈希等敏感字段。
 */
public record PlayerProfile(
        String id,
        String username,
        String nickname,
        String email,
        String phone,
        String avatarUrl,
        String school,
        String status,
        Instant lastLoginAt,
        Instant createdAt
) {

    public static PlayerProfile from(AppUser user) {
        return new PlayerProfile(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getSchool(),
                user.getStatus(),
                TimeUtils.toInstant(user.getLastLoginAt()),
                TimeUtils.toInstant(user.getCreatedAt()));
    }
}
