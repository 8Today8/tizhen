package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增学习阶段请求。阶段必须归属于一条未删除路线。
 */
public record CreateStageRequest(
        @NotNull(message = "路线 ID 不能为空")
        Long routeId,

        @NotBlank(message = "阶段名称不能为空")
        @Size(max = 100, message = "阶段名称不能超过 100 个字符")
        String name,

        @Size(max = 200_000, message = "阶段内容不能超过 200000 个字符")
        String content,

        Integer sortOrder
) {
}
