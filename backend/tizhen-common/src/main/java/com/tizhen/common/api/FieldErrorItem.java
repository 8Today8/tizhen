package com.tizhen.common.api;

/**
 * 参数校验失败的字段级错误，字段名使用 API 的 camelCase 名称。
 */
public record FieldErrorItem(String field, String message) {
}
