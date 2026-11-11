package com.tizhen.core.account.model;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

/**
 * 选手会话中的身份快照。实现 Principal 让 Spring Security 的认证名称稳定为用户名，
 * 便于 Spring Session 按 principalName 建立会话索引。
 */
public record PlayerPrincipal(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String school
) implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return username;
    }

    public static PlayerPrincipal from(PlayerProfile profile) {
        return new PlayerPrincipal(
                Long.valueOf(profile.id()),
                profile.username(),
                profile.nickname(),
                profile.avatarUrl(),
                profile.school());
    }
}
