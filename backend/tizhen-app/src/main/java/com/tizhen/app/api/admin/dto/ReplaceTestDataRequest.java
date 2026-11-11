package com.tizhen.app.api.admin.dto;

import com.tizhen.core.problem.model.TestDataInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 全量替换 OJ 测试数据请求。数组顺序即测试点顺序。
 */
public record ReplaceTestDataRequest(
        @NotNull(message = "测试数据列表不能为空")
        @Size(min = 1, max = 500, message = "测试数据必须为 1–500 组")
        List<@Valid TestDataItemRequest> items
) {

    public List<TestDataInput> toCommand() {
        return items.stream()
                .map(item -> item == null ? null : new TestDataInput(
                        item.inputData(), item.outputData()))
                .toList();
    }
}
