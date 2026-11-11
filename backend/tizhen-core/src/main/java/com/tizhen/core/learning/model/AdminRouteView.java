package com.tizhen.core.learning.model;

import java.time.Instant;

/**
 * 后台学习路线列表项。
 */
public record AdminRouteView(
        String id,
        String name,
        String description,
        String coverUrl,
        Integer sortOrder,
        String status,
        long stageCount,
        Instant createdAt,
        Instant updatedAt
) {
}
