package com.tizhen.core.learning.model;

import java.time.Instant;
import java.util.List;

/**
 * 后台阶段详情，包含草稿/下架课程与全部阶段题目关联。
 */
public record AdminStageDetail(
        String id,
        String routeId,
        String routeName,
        String name,
        String content,
        Integer sortOrder,
        String status,
        List<AdminCourseView> courses,
        List<AdminStageProblemView> problems,
        Instant createdAt,
        Instant updatedAt
) {

    public AdminStageDetail {
        courses = courses == null ? List.of() : List.copyOf(courses);
        problems = problems == null ? List.of() : List.copyOf(problems);
    }
}
