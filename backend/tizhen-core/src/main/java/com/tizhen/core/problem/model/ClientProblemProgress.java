package com.tizhen.core.problem.model;

import java.time.Instant;

/**
 * 当前用户在某题上的作答摘要。
 */
public record ClientProblemProgress(
        long attemptCount,
        boolean firstAc,
        Instant firstAcAt,
        String lastVerdict,
        Instant lastSubmittedAt
) {
}
