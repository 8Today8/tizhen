package com.tizhen.app.api.admin.dto;

import com.tizhen.core.problem.model.TestDataView;

import java.util.List;

/**
 * 全量替换测试数据后的响应。
 */
public record TestDataListView(List<TestDataView> items) {

    public TestDataListView {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
