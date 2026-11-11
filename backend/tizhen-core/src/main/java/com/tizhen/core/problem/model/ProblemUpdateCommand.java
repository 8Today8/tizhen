package com.tizhen.core.problem.model;

import java.util.List;
import java.util.Set;

/**
 * 修改题目命令。presentFields 记录请求中实际出现的字段，
 * 用于区分“未提交”与“显式清空”；tags/samples/options 出现即整体替换。
 */
public record ProblemUpdateCommand(
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
        String platform,
        Set<String> presentFields
) {

    public ProblemUpdateCommand {
        presentFields = presentFields == null ? Set.of() : Set.copyOf(presentFields);
    }

    public boolean has(String field) {
        return presentFields.contains(field);
    }

    public boolean isEmpty() {
        return presentFields.isEmpty();
    }
}
