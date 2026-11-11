package com.tizhen.core.problem.model;

/**
 * 后台测试数据视图。测试数据正文只对管理员返回。
 */
public record TestDataView(
        String id,
        String inputData,
        String outputData,
        Integer sortOrder
) {
}
