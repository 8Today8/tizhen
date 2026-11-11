package com.tizhen.app.api.admin.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.tizhen.core.learning.model.RouteUpdateCommand;
import jakarta.validation.constraints.Size;

/**
 * 修改学习路线请求。setter 记录字段是否出现，以区分未提交与显式清空。
 */
public class UpdateRouteRequest {

    @Size(max = 100, message = "路线名称不能超过 100 个字符")
    private String name;
    private boolean namePresent;

    @Size(max = 10_000, message = "路线简介不能超过 10000 个字符")
    private String description;
    private boolean descriptionPresent;

    @Size(max = 512, message = "封面地址不能超过 512 个字符")
    private String coverUrl;
    private boolean coverUrlPresent;

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

    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    public boolean isDescriptionPresent() {
        return descriptionPresent;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    @JsonSetter("coverUrl")
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
        this.coverUrlPresent = true;
    }

    public boolean isCoverUrlPresent() {
        return coverUrlPresent;
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

    public RouteUpdateCommand toCommand() {
        return new RouteUpdateCommand(
                name, namePresent,
                description, descriptionPresent,
                coverUrl, coverUrlPresent,
                sortOrder, sortOrderPresent);
    }
}
