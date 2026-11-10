package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank(message = "请输入管理员账号")
        @Size(max = 32, message = "管理员账号长度不能超过 32 位")
        String username,

        @NotBlank(message = "请输入密码")
        @Size(max = 64, message = "密码长度不能超过 64 位")
        String password
) {
}
