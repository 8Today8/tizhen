package com.tizhen.core.learning.model;

/**
 * 已发布路线摘要与当前选手的阶段完成进度。
 */
public record RouteSummary(
        String id,
        String name,
        String description,
        String coverUrl,
        Integer sortOrder,
        long stageCount,
        long completedStageCount,
        int progressPercent
) {
}
