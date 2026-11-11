package com.tizhen.core.record.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 错题本列表项。latestScore 仅简答题有值，其他题型为 null。
 */
public record WrongBookItem(
        String id,
        WrongBookProblemBrief problem,
        String latestError,
        BigDecimal latestScore,
        int errorCount,
        Instant lastErrorAt
) {
}
