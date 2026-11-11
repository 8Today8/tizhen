package com.tizhen.core.learning.model;

import java.time.Instant;

/**
 * 外链题完成状态更新结果。
 */
public record ExternalCompletionView(
        String problemId,
        boolean completed,
        String completionSource,
        Instant completedAt
) {
}
