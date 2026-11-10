package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增内容运营账号请求。
 */
public record CreateAdminAccountRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "用户名需为 3–20 位字母、数字或下划线")
        String username,

        @NotBlank
        @Size(max = 64, message = "显示名称不能超过 64 个字符")
        String displayName,

        @NotBlank
        @Size(min = 6, max = 64, message = "密码需为 6–64 个字符")
        String password,

        @NotBlank
        String roleCode
) {
}
