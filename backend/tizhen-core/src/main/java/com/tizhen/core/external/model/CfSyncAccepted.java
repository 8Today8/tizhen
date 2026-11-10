package com.tizhen.core.external.model;

import java.time.Instant;

/**
 * CF 模拟刷新受理结果。
 */
public record CfSyncAccepted(
        boolean accepted,
        int accountCount,
        Instant acceptedAt
) {
}
