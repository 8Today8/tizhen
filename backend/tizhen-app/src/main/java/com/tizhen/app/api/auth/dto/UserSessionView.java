package com.tizhen.app.api.auth.dto;

import com.tizhen.core.account.model.PlayerProfile;

/**
 * 登录或注册成功后的会话快照。
 */
public record UserSessionView(
        PlayerProfile user,
        String csrfToken
) {
}
