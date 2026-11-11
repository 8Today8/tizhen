package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.Size;

/**
 * 更换用户头像请求。avatarUrl 为空表示清除头像。
 */
public record UpdateUserAvatarRequest(
        @Size(max = 512, message = "头像地址不能超过 512 个字符")
        String avatarUrl
) {
}
