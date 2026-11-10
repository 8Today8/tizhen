package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 用户启停用请求。
 */
public record UpdateUserStatusRequest(
        @NotBlank
        @Pattern(regexp = "active|disabled", message = "status 仅支持 active 或 disabled")
        String status
) {
}
