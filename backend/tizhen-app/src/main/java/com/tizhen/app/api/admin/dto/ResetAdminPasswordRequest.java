package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置管理员密码请求。密码只用于哈希，不写日志。
 */
public record ResetAdminPasswordRequest(
        @NotBlank
        @Size(min = 6, max = 64, message = "新密码需为 6–64 个字符")
        String newPassword
) {
}
