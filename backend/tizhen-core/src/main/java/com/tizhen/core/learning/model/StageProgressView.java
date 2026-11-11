package com.tizhen.core.learning.model;

import java.time.Instant;

/**
 * 阶段完成状态更新结果。
 */
public record StageProgressView(
        String stageId,
        boolean completed,
        Instant completedAt
) {
}
