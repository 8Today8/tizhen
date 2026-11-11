package com.tizhen.app.api.problem.dto;

import java.util.List;

/**
 * 选手提交请求。按题型只使用对应字段，具体校验由提交服务完成。
 */
public record SubmissionCreateRequest(
        String language,
        String code,
        List<String> selectedOptions,
        String answerText
) {
}
