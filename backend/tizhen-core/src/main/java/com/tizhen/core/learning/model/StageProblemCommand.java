package com.tizhen.core.learning.model;

/**
 * 阶段题目关联输入项。
 */
public record StageProblemCommand(
        Long problemId,
        Integer sortOrder
) {
}
