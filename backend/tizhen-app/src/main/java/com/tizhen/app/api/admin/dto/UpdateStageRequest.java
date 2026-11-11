package com.tizhen.app.api.admin.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.tizhen.core.learning.model.StageUpdateCommand;
import jakarta.validation.constraints.Size;

/**
 * 修改学习阶段请求。不支持修改 routeId，跨路线移动请重新创建阶段。
 */
public class UpdateStageRequest {

    @Size(max = 100, message = "阶段名称不能超过 100 个字符")
    private String name;
    private boolean namePresent;

    @Size(max = 200_000, message = "阶段内容不能超过 200000 个字符")
    private String content;
    private boolean contentPresent;

    private Integer sortOrder;
    private boolean sortOrderPresent;

    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
        this.namePresent = true;
    }

    public boolean isNamePresent() {
        return namePresent;
    }

    public String getContent() {
        return content;
    }

    @JsonSetter("content")
    public void setContent(String content) {
        this.content = content;
        this.contentPresent = true;
    }

    public boolean isContentPresent() {
        return contentPresent;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    @JsonSetter("sortOrder")
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        this.sortOrderPresent = true;
    }

    public boolean isSortOrderPresent() {
        return sortOrderPresent;
    }

    public StageUpdateCommand toCommand() {
        return new StageUpdateCommand(
                name, namePresent,
                content, contentPresent,
                sortOrder, sortOrderPresent);
    }
}
