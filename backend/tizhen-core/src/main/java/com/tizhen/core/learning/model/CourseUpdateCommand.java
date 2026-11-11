package com.tizhen.core.learning.model;

/**
 * 修改阶段课程命令。present 标记用于区分“未提交”和“显式清空”。
 */
public record CourseUpdateCommand(
        String title,
        boolean titlePresent,
        String url,
        boolean urlPresent,
        Integer durationSeconds,
        boolean durationSecondsPresent,
        String source,
        boolean sourcePresent,
        Integer sortOrder,
        boolean sortOrderPresent,
        String status,
        boolean statusPresent
) {
}
