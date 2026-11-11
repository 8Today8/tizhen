package com.tizhen.core.judge.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提交历史联表查询行。
 */
public class SubmissionListRow {

    private Long id;
    private Long problemId;
    private String problemType;
    private String problemTitle;
    private Long battleId;
    private String language;
    private String status;
    private String verdict;
    private BigDecimal score;
    private LocalDateTime createdAt;
    private LocalDateTime judgedAt;

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

    public String getProblemTitle() {
        return problemTitle;
    }

    public void setProblemTitle(String problemTitle) {
        this.problemTitle = problemTitle;
    }

    public Long getBattleId() {
        return battleId;
    }

    public void setBattleId(Long battleId) {
        this.battleId = battleId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getJudgedAt() {
        return judgedAt;
    }

    public void setJudgedAt(LocalDateTime judgedAt) {
        this.judgedAt = judgedAt;
    }
}
