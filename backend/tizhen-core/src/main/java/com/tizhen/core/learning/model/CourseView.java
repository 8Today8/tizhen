package com.tizhen.core.learning.model;

/**
 * 选手端阶段课程。只暴露跳转所需信息，不返回管理状态。
 */
public record CourseView(
        String id,
        String title,
        String url,
        Integer durationSeconds,
        String source,
        Integer sortOrder
) {
}
