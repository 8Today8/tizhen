package com.tizhen.core.problem.model;

import java.time.Instant;
import java.util.List;

/**
 * 后台题目列表项，供阶段题目选择器使用。不包含题面与答案配置。
 */
public record AdminProblemSummary(
        String id,
        String type,
        String objectiveType,
        String title,
        String difficulty,
        String source,
        String platform,
        String status,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {

    public AdminProblemSummary {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
