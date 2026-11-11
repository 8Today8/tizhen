package com.tizhen.app.api.admin.dto;

import com.tizhen.core.learning.model.AdminStageProblemView;

import java.util.List;

/**
 * 阶段题目全量替换后的响应包装。
 */
public record StageProblemListView(List<AdminStageProblemView> items) {

    public StageProblemListView {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
