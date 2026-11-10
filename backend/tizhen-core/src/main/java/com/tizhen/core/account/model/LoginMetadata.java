package com.tizhen.core.account.model;

/**
 * 登录审计与限流所需的请求元数据。
 */
public record LoginMetadata(String ipAddress, String userAgent, String requestId) {
}
