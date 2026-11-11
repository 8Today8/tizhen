package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.Size;

/**
 * 客观题选项请求。
 */
public record ObjectiveOptionRequest(
        @Size(max = 1, message = "选项键必须是单个字母")
        String optionKey,

        String content,

        Boolean isCorrect,

        Integer sortOrder
) {
}
