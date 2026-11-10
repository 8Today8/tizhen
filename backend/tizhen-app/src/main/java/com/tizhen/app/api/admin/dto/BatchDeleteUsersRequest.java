package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量删除用户请求。单次最多处理 100 位用户。
 */
public record BatchDeleteUsersRequest(
        @NotEmpty(message = "请选择要删除的用户")
        @Size(max = 100, message = "单次最多删除 100 位用户")
        List<@NotNull(message = "用户 ID 不能为空") Long> userIds
) {
}
