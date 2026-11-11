package com.tizhen.app.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 选手登录请求。用户名大小写不敏感由数据库排序规则保证。
 */
public record PlayerLoginRequest(
        @NotBlank(message = "请输入用户名")
        @Size(max = 20, message = "用户名不能超过 20 个字符")
        String username,

        @NotBlank(message = "请输入密码")
        @Size(max = 64, message = "密码不能超过 64 个字符")
        String password
) {
}
