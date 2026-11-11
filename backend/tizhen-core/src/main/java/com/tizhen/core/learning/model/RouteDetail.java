package com.tizhen.core.learning.model;

import java.util.List;

/**
 * 路线详情：路线摘要 + 已发布阶段。
 */
public record RouteDetail(
        String id,
        String name,
        String description,
        String coverUrl,
        Integer sortOrder,
        long stageCount,
        long completedStageCount,
        int progressPercent,
        List<StageSummary> stages
) {
}
