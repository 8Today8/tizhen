package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.Size;

/**
 * 题面样例请求。样例正文保留空白字符。
 */
public record ProblemSampleRequest(
        String inputData,
        String outputData,
        @Size(max = 100_000, message = "样例说明不能超过 100000 个字符")
        String explanation,
        Integer sortOrder
) {
}
