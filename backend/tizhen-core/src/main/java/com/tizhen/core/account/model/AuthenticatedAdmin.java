package com.tizhen.core.account.model;

/**
 * 登录成功后的管理员身份与后台会话令牌。令牌只允许写入 HttpOnly Cookie。
 */
public record AuthenticatedAdmin(AdminPrincipal principal, String sessionToken) {
}
