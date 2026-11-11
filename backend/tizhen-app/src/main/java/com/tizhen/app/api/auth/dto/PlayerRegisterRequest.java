package com.tizhen.app.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 选手注册请求。密码只用于哈希，不返回、不记录。
 */
public record PlayerRegisterRequest(
        @NotBlank(message = "请输入用户名")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "用户名需为 3–20 位字母、数字或下划线")
        String username,

        @NotBlank(message = "请输入昵称")
        @Size(max = 64, message = "昵称不能超过 64 个字符")
        String nickname,

        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 64, message = "密码需为 6–64 个字符")
        String password,

        @NotBlank(message = "请再次输入密码")
        @Size(max = 64, message = "确认密码不能超过 64 个字符")
        String confirmPassword
) {
}
