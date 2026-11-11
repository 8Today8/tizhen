package com.tizhen.core.problem.model;

/**
 * 后台题目详情中的客观题选项，包含正确答案标记。
 */
public record ObjectiveOptionView(
        String id,
        String optionKey,
        String content,
        Boolean isCorrect,
        Integer sortOrder
) {
}
