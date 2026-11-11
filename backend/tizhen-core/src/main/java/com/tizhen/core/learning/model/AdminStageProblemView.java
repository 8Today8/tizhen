package com.tizhen.core.learning.model;

import com.tizhen.core.problem.model.AdminProblemBrief;

/**
 * 阶段题目关联视图，sortOrder 表示题目在阶段内的顺序。
 */
public record AdminStageProblemView(
        AdminProblemBrief problem,
        int sortOrder
) {
}
