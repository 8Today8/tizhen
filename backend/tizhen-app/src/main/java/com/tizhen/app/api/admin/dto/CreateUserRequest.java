package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 超级管理员新增选手账号请求。密码只用于哈希，不写日志。
 */
public record CreateUserRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "用户名需为 3–20 位字母、数字或下划线")
        String username,

        @NotBlank
        @Size(max = 64, message = "昵称不能超过 64 个字符")
        String nickname,

        @NotBlank
        @Size(min = 6, max = 64, message = "密码需为 6–64 个字符")
        String password,

        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱不能超过 254 个字符")
        String email,

        @Size(max = 32, message = "手机号不能超过 32 个字符")
        String phone,

        @Size(max = 128, message = "学校不能超过 128 个字符")
        String school,

        @Size(max = 512, message = "头像地址不能超过 512 个字符")
        String avatarUrl
) {
}
