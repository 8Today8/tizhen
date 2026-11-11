package com.tizhen.app.api.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 全量替换阶段题目请求。空数组表示解除全部关联。
 */
public record ReplaceStageProblemsRequest(
        @NotNull(message = "题目列表不能为空")
        @Size(max = 200, message = "单个阶段最多关联 200 道题目")
        List<@Valid StageProblemItemRequest> items
) {

    public record StageProblemItemRequest(
            @NotNull(message = "题目 ID 不能为空")
            Long problemId,

            Integer sortOrder
    ) {
    }
}
