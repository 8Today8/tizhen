package com.tizhen.core.problem.model;

import java.time.LocalDateTime;

/**
 * 选手端题目查询行。包含当前用户的首次 AC、提交与外链完成状态，
 * 避免列表渲染逐题查询。
 */
public class ClientProblemRow {

    private Long id;
    private String type;
    private String objectiveType;
    private String title;
    private String statement;
    private String inputFormat;
    private String outputFormat;
    private String difficulty;
    private String source;
    private String externalUrl;
    private String platform;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private Integer maxScore;
    private Integer solvedByMe;
    private Integer attemptedByMe;
    private Integer externalCompleted;
    private LocalDateTime firstAcAt;
    private LocalDateTime lastSubmittedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
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

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Integer getTimeLimitMs() {
        return timeLimitMs;
    }

    public void setTimeLimitMs(Integer timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
    }

    public Integer getMemoryLimitMb() {
        return memoryLimitMb;
    }

    public void setMemoryLimitMb(Integer memoryLimitMb) {
        this.memoryLimitMb = memoryLimitMb;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public Integer getSolvedByMe() {
        return solvedByMe;
    }

    public void setSolvedByMe(Integer solvedByMe) {
        this.solvedByMe = solvedByMe;
    }

    public Integer getAttemptedByMe() {
        return attemptedByMe;
    }

    public void setAttemptedByMe(Integer attemptedByMe) {
        this.attemptedByMe = attemptedByMe;
    }

    public Integer getExternalCompleted() {
        return externalCompleted;
    }

    public void setExternalCompleted(Integer externalCompleted) {
        this.externalCompleted = externalCompleted;
    }

    public LocalDateTime getFirstAcAt() {
        return firstAcAt;
    }

    public void setFirstAcAt(LocalDateTime firstAcAt) {
        this.firstAcAt = firstAcAt;
    }

    public LocalDateTime getLastSubmittedAt() {
        return lastSubmittedAt;
    }

    public void setLastSubmittedAt(LocalDateTime lastSubmittedAt) {
        this.lastSubmittedAt = lastSubmittedAt;
    }
}
