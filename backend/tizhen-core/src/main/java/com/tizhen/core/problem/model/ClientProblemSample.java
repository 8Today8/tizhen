package com.tizhen.core.problem.model;

/**
 * 选手端样例。与判题测试数据分离，仅展示题面样例。
 */
public record ClientProblemSample(
        String inputData,
        String outputData,
        String explanation,
        Integer sortOrder
) {
}
