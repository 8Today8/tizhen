package com.tizhen.core.problem.model;

import java.time.Instant;
import java.util.List;

/**
 * 后台题目详情。包含题型配置、引用统计与是否可删除，不包含测试数据正文。
 */
public record AdminProblemDetail(
        String id,
        String type,
        String objectiveType,
        String title,
        String statement,
        String difficulty,
        String source,
        List<String> tags,
        String inputFormat,
        String outputFormat,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        List<ProblemSampleView> samples,
        List<ObjectiveOptionView> options,
        String explanation,
        String referenceAnswer,
        List<ScoringRubricItem> scoringRubric,
        Integer maxScore,
        String externalUrl,
        String platform,
        long testDataCount,
        String status,
        Instant createdAt,
        Instant updatedAt,
        AdminProblemUsage usage,
        boolean canDelete
) {

    public AdminProblemDetail {
        tags = tags == null ? List.of() : List.copyOf(tags);
        samples = samples == null ? List.of() : List.copyOf(samples);
        options = options == null ? List.of() : List.copyOf(options);
        scoringRubric = scoringRubric == null ? List.of() : List.copyOf(scoringRubric);
    }
}
