package com.tizhen.app.api.admin.dto;

/**
 * 单组测试数据请求。空输入是合法输入，因此只校验非 null。
 */
public record TestDataItemRequest(
        String inputData,
        String outputData
) {
}
