package com.tizhen.core.learning.model;

import java.time.Instant;
import java.util.List;

/**
 * 路线详情中的阶段摘要，包含课程预览。
 */
public record StageSummary(
        String id,
        String name,
        Integer sortOrder,
        boolean completed,
        Instant completedAt,
        long courseCount,
        long problemCount,
        List<CourseView> courses
) {
}
