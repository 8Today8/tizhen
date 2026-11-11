package com.tizhen.core.learning.model;

import java.time.Instant;
import java.util.List;

/**
 * 选手端阶段详情。
 */
public record StageDetail(
        String id,
        String routeId,
        String routeName,
        String name,
        String content,
        boolean completed,
        Instant completedAt,
        List<CourseView> courses,
        List<StageProblemView> problems
) {
}
