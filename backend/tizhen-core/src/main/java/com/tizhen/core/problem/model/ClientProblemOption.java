package com.tizhen.core.problem.model;

/**
 * 选手端客观题选项，不包含正确答案标记。
 */
public record ClientProblemOption(
        String optionKey,
        String content,
        Integer sortOrder
) {
}
