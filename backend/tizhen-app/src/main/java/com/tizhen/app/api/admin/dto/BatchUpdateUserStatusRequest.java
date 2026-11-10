package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量启用或禁用用户请求。单次最多处理 100 位用户。
 */
public record BatchUpdateUserStatusRequest(
        @NotEmpty(message = "请选择要操作的用户")
        @Size(max = 100, message = "单次最多操作 100 位用户")
        List<@NotNull(message = "用户 ID 不能为空") Long> userIds,

        @NotBlank
        @Pattern(regexp = "active|disabled", message = "status 仅支持 active 或 disabled")
        String status
) {
}
