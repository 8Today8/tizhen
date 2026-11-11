package com.tizhen.core.learning.model;

import java.time.Instant;
import java.util.List;

/**
 * 后台路线详情，包含全部未删除阶段（草稿与下架阶段也返回）。
 */
public record AdminRouteDetail(
        String id,
        String name,
        String description,
        String coverUrl,
        Integer sortOrder,
        String status,
        long stageCount,
        Instant createdAt,
        Instant updatedAt,
        List<AdminStageSummary> stages
) {

    public AdminRouteDetail {
        stages = stages == null ? List.of() : List.copyOf(stages);
    }
}
