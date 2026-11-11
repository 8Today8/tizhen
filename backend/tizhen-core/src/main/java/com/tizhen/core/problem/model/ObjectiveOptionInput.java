package com.tizhen.core.problem.model;

/**
 * 客观题选项输入。
 */
public record ObjectiveOptionInput(
        String optionKey,
        String content,
        Boolean isCorrect,
        Integer sortOrder
) {
}
