package com.tizhen.app.api.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 简答题评分要点请求。
 */
public record ScoringRubricRequest(
        @Size(max = 200, message = "评分要点不能超过 200 个字符")
        String point,

        @DecimalMin(value = "0", message = "评分要点分数不能为负数")
        BigDecimal score,

        @Size(max = 500, message = "评分说明不能超过 500 个字符")
        String description
) {
}
