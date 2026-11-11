package com.tizhen.core.problem.model;

import java.util.List;

/**
 * 选手端题目列表项。不包含答案、测试数据与评分要点。
 */
public record ClientProblemSummary(
        String id,
        String type,
        String objectiveType,
        String title,
        String difficulty,
        String source,
        String platform,
        String externalUrl,
        List<String> tags,
        boolean solvedByMe,
        boolean attemptedByMe,
        Boolean externalCompleted
) {
}
