package com.tizhen.app.api.admin.dto;

/**
 * 前端获取 CSRF Token 的响应；同时由服务端写入对应 Cookie。
 */
public record CsrfTokenView(String token, String headerName, String cookieName) {
}
