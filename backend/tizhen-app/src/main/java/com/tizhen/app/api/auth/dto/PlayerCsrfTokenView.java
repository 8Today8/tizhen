package com.tizhen.app.api.auth.dto;

/**
 * 选手端 CSRF 初始化结果。前端从 Cookie 读取 token 并放入请求头。
 */
public record PlayerCsrfTokenView(
        String token,
        String headerName,
        String cookieName
) {
}
