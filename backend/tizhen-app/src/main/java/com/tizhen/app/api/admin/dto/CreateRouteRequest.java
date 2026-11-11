package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增学习路线请求。状态固定由服务端设置为草稿。
 */
public record CreateRouteRequest(
        @NotBlank(message = "路线名称不能为空")
        @Size(max = 100, message = "路线名称不能超过 100 个字符")
        String name,

        @Size(max = 10_000, message = "路线简介不能超过 10000 个字符")
        String description,

        @Size(max = 512, message = "封面地址不能超过 512 个字符")
        String coverUrl,

        Integer sortOrder
) {
}
