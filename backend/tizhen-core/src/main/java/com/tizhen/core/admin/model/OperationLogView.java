package com.tizhen.core.admin.model;

import java.time.Instant;

/**
 * 操作日志视图。extra 已按 JSON 对象返回，敏感字段不得写入。
 */
public record OperationLogView(
        String id,
        AdminLogUser admin,
        String action,
        String targetType,
        String targetId,
        String detail,
        Object extra,
        String ipAddress,
        String userAgent,
        String requestId,
        Instant createdAt
) {
}
