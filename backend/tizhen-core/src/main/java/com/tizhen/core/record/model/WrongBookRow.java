package com.tizhen.core.record.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 错题本联表查询行，由 MyBatis 映射。
 */
public class WrongBookRow {

    private Long id;
    private Long problemId;
    private String problemType;
    private String title;
    private String difficulty;
    private String latestError;
    private BigDecimal latestScore;
    private Integer errorCount;
    private LocalDateTime lastErrorAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public String getProblemType() {
        return problemType;
    }

    public void setProblemType(String problemType) {
        this.problemType = problemType;
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

    public String getLatestError() {
        return latestError;
    }

    public void setLatestError(String latestError) {
        this.latestError = latestError;
    }

    public BigDecimal getLatestScore() {
        return latestScore;
    }

    public void setLatestScore(BigDecimal latestScore) {
        this.latestScore = latestScore;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public LocalDateTime getLastErrorAt() {
        return lastErrorAt;
    }

    public void setLastErrorAt(LocalDateTime lastErrorAt) {
        this.lastErrorAt = lastErrorAt;
    }
}
