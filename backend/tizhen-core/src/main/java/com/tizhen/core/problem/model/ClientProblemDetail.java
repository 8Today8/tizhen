package com.tizhen.core.problem.model;

import java.util.List;

/**
 * 选手端题目详情。答案类字段只在提交详情中按判题完成状态返回。
 */
public record ClientProblemDetail(
        String id,
        String type,
        String objectiveType,
        String title,
        String statement,
        String inputFormat,
        String outputFormat,
        String difficulty,
        String source,
        List<String> tags,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        Integer maxScore,
        String externalUrl,
        String platform,
        Boolean externalCompleted,
        List<ClientProblemSample> samples,
        List<ClientProblemOption> options,
        ClientProblemProgress myProgress
) {
}
