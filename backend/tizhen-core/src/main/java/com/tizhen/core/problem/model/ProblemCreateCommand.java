package com.tizhen.core.problem.model;

import java.util.List;

/**
 * 新增题目命令。题型特有字段由服务层按 type 校验。
 */
public record ProblemCreateCommand(
        String type,
        String title,
        String statement,
        String difficulty,
        String source,
        List<String> tags,
        String inputFormat,
        String outputFormat,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        List<ProblemSampleInput> samples,
        String objectiveType,
        List<ObjectiveOptionInput> options,
        String explanation,
        String referenceAnswer,
        List<ScoringRubricItem> scoringRubric,
        Integer maxScore,
        String externalUrl,
        String platform
) {
}
