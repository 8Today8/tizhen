package com.tizhen.app.api.problem.dto;

import com.tizhen.core.problem.model.ProblemTagOption;

import java.util.List;

/**
 * 专题标签筛选选项响应。
 */
public record ProblemTagListView(List<ProblemTagOption> items) {
}
