package com.tizhen.core.problem.model;

/**
 * 后台题目详情中的样例。
 */
public record ProblemSampleView(
        String id,
        String inputData,
        String outputData,
        String explanation,
        Integer sortOrder
) {
}
