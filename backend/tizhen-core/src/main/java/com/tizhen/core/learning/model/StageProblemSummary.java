package com.tizhen.core.learning.model;

/**
 * 选手端题目摘要。不包含题面、答案、测试数据等字段。
 */
public record StageProblemSummary(
        String id,
        String type,
        String objectiveType,
        String title,
        String difficulty,
        String source,
        String platform,
        String externalUrl
) {
}
