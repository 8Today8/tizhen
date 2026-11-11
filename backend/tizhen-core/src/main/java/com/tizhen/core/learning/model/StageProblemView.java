package com.tizhen.core.learning.model;

import java.time.Instant;

/**
 * 阶段题目视图。外链题携带手动完成状态，其他题型该字段为空。
 */
public record StageProblemView(
        StageProblemSummary problem,
        Integer sortOrder,
        Boolean externalCompleted,
        Instant completedAt
) {
}
