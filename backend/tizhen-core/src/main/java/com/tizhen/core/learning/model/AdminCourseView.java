package com.tizhen.core.learning.model;

/**
 * 后台课程视图。status 用于后台管理草稿/下架课程，前台 CourseView 不返回该字段。
 */
public record AdminCourseView(
        String id,
        String title,
        String url,
        Integer durationSeconds,
        String source,
        Integer sortOrder,
        String status
) {
}
