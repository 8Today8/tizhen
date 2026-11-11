package com.tizhen.core.learning.model;

/**
 * 新增阶段课程命令。
 */
public record CourseCreateCommand(
        String title,
        String url,
        Integer durationSeconds,
        String source,
        Integer sortOrder,
        String status
) {
}
