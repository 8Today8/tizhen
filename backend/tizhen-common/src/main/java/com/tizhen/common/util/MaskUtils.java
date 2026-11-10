package com.tizhen.common.util;

/**
 * 敏感信息脱敏工具。API Key、令牌等不得以明文进入日志或响应。
 */
public final class MaskUtils {

    private MaskUtils() {
    }

    public static String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        String trimmed = secret.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }
}
