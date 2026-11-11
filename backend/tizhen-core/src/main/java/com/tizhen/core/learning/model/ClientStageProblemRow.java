package com.tizhen.core.learning.model;

import java.time.LocalDateTime;

/**
 * 选手端阶段题目查询行。答案、测试数据等敏感字段不进入该模型。
 */
public class ClientStageProblemRow {

    private Long problemId;

    private Integer sortOrder;

    private String type;

    private String objectiveType;

    private String title;

    private String difficulty;

    private String source;

    private String platform;

    private String externalUrl;

    private Boolean externalCompleted;

    private LocalDateTime externalCompletedAt;

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getObjectiveType() {
        return objectiveType;
    }

    public void setObjectiveType(String objectiveType) {
        this.objectiveType = objectiveType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public Boolean getExternalCompleted() {
        return externalCompleted;
    }

    public void setExternalCompleted(Boolean externalCompleted) {
        this.externalCompleted = externalCompleted;
    }

    public LocalDateTime getExternalCompletedAt() {
        return externalCompletedAt;
    }

    public void setExternalCompletedAt(LocalDateTime externalCompletedAt) {
        this.externalCompletedAt = externalCompletedAt;
    }
}
