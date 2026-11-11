package com.tizhen.core.record.model;

/**
 * 错题本中的题目摘要，只暴露复习所需的最小信息。
 */
public record WrongBookProblemBrief(
        String id,
        String type,
        String title,
        String difficulty
) {
}
