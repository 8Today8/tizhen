package com.tizhen.core.problem.model;

import java.math.BigDecimal;

/**
 * 简答题评分要点。创建、修改和详情共用同一结构。
 */
public record ScoringRubricItem(
        String point,
        BigDecimal score,
        String description
) {
}
