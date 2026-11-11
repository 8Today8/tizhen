package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增阶段课程请求。课程链接在服务端规范化并校验为 B 站视频。
 */
public record CreateCourseRequest(
        @NotBlank(message = "课程标题不能为空")
        @Size(max = 200, message = "课程标题不能超过 200 个字符")
        String title,

        @NotBlank(message = "课程链接不能为空")
        @Size(max = 512, message = "课程链接不能超过 512 个字符")
        String url,

        @Min(value = 0, message = "课程时长不能为负数")
        Integer durationSeconds,

        @Size(max = 128, message = "课程来源不能超过 128 个字符")
        String source,

        Integer sortOrder,

        @Pattern(regexp = "draft|published|offline", message = "status 仅支持 draft、published 或 offline")
        String status
) {
}
