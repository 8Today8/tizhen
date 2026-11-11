package com.tizhen.app.api.learning.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 阶段或外链题完成状态更新请求。
 */
public class ProgressUpdateRequest {

    @NotNull(message = "完成状态不能为空")
    private Boolean completed;

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
