package com.tizhen.core.learning.model;

import java.time.Instant;

/**
 * 后台阶段列表项。
 */
public record AdminStageSummary(
        String id,
        String routeId,
        String routeName,
        String name,
        Integer sortOrder,
        String status,
        long courseCount,
        long problemCount,
        Instant createdAt,
        Instant updatedAt
) {
}
