package com.tizhen.core.judge.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 提交历史列表项，不包含代码、答案与 AI 评语。
 */
public record SubmissionListItem(
        String id,
        SubmissionProblemBrief problem,
        String battleId,
        String language,
        String status,
        String verdict,
        BigDecimal score,
        Instant createdAt,
        Instant judgedAt
) {
}
