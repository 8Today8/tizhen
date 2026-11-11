package com.tizhen.app.api.admin.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.tizhen.core.learning.model.CourseUpdateCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 修改阶段课程请求。setter 记录字段是否出现，以区分未提交与显式清空。
 */
public class UpdateCourseRequest {

    @Size(max = 200, message = "课程标题不能超过 200 个字符")
    private String title;
    private boolean titlePresent;

    @Size(max = 512, message = "课程链接不能超过 512 个字符")
    private String url;
    private boolean urlPresent;

    @Min(value = 0, message = "课程时长不能为负数")
    private Integer durationSeconds;
    private boolean durationSecondsPresent;

    @Size(max = 128, message = "课程来源不能超过 128 个字符")
    private String source;
    private boolean sourcePresent;

    private Integer sortOrder;
    private boolean sortOrderPresent;

    @Size(max = 16, message = "课程状态不能超过 16 个字符")
    private String status;
    private boolean statusPresent;

    public String getTitle() {
        return title;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    public boolean isTitlePresent() {
        return titlePresent;
    }

    public String getUrl() {
        return url;
    }

    @JsonSetter("url")
    public void setUrl(String url) {
        this.url = url;
        this.urlPresent = true;
    }

    public boolean isUrlPresent() {
        return urlPresent;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    @JsonSetter("durationSeconds")
    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
        this.durationSecondsPresent = true;
    }

    public boolean isDurationSecondsPresent() {
        return durationSecondsPresent;
    }

    public String getSource() {
        return source;
    }

    @JsonSetter("source")
    public void setSource(String source) {
        this.source = source;
        this.sourcePresent = true;
    }

    public boolean isSourcePresent() {
        return sourcePresent;
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

    public String getStatus() {
        return status;
    }

    @JsonSetter("status")
    public void setStatus(String status) {
        this.status = status;
        this.statusPresent = true;
    }

    public boolean isStatusPresent() {
        return statusPresent;
    }

    public CourseUpdateCommand toCommand() {
        return new CourseUpdateCommand(
                title, titlePresent,
                url, urlPresent,
                durationSeconds, durationSecondsPresent,
                source, sourcePresent,
                sortOrder, sortOrderPresent,
                status, statusPresent);
    }
}
