package com.tizhen.core.problem.model;

/**
 * 阶段题目关联中使用的题目摘要，不包含题面与答案配置。
 */
public record AdminProblemBrief(
        String id,
        String type,
        String objectiveType,
        String title,
        String difficulty,
        String source,
        String platform,
        String status
) {
}
