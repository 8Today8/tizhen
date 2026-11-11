package com.tizhen.core.judge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 错题本聚合记录。按用户与题目唯一，手动移除后再次出错会重新激活。
 */
@TableName("wrong_book")
public class WrongBook {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long problemId;

    private String latestError;

    private BigDecimal latestScore;

    private Integer errorCount;

    private LocalDateTime lastErrorAt;

    private LocalDateTime removedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
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

    public LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(LocalDateTime removedAt) {
        this.removedAt = removedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
