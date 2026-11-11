package com.tizhen.core.problem.model;

/**
 * 题面样例输入。样例正文保留空白字符，不做 trim。
 */
public record ProblemSampleInput(
        String inputData,
        String outputData,
        String explanation,
        Integer sortOrder
) {
}
