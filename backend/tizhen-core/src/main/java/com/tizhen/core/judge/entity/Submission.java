package com.tizhen.core.judge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通刷题与对战共用的提交记录。判题结果由 worker 回写。
 */
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long problemId;

    private String problemType;

    private Long battleId;

    private String language;

    private String code;

    /** 客观题选项键的 JSON 数组文本。 */
    private String answerJson;

    private String answerText;

    private String status;

    private String verdict;

    private BigDecimal score;

    private String aiFeedback;

    private String failureReason;

    private String judgeDetail;

    private String compileOutput;

    private Integer judgeTimeMs;

    private Integer memoryKb;

    private Integer judgeAttempts;

    private LocalDateTime judgeStartedAt;

    private LocalDateTime judgedAt;

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

    public String getProblemType() {
        return problemType;
    }

    public void setProblemType(String problemType) {
        this.problemType = problemType;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAnswerJson() {
        return answerJson;
    }

    public void setAnswerJson(String answerJson) {
        this.answerJson = answerJson;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
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

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getJudgeDetail() {
        return judgeDetail;
    }

    public void setJudgeDetail(String judgeDetail) {
        this.judgeDetail = judgeDetail;
    }

    public String getCompileOutput() {
        return compileOutput;
    }

    public void setCompileOutput(String compileOutput) {
        this.compileOutput = compileOutput;
    }

    public Integer getJudgeTimeMs() {
        return judgeTimeMs;
    }

    public void setJudgeTimeMs(Integer judgeTimeMs) {
        this.judgeTimeMs = judgeTimeMs;
    }

    public Integer getMemoryKb() {
        return memoryKb;
    }

    public void setMemoryKb(Integer memoryKb) {
        this.memoryKb = memoryKb;
    }

    public Integer getJudgeAttempts() {
        return judgeAttempts;
    }

    public void setJudgeAttempts(Integer judgeAttempts) {
        this.judgeAttempts = judgeAttempts;
    }

    public LocalDateTime getJudgeStartedAt() {
        return judgeStartedAt;
    }

    public void setJudgeStartedAt(LocalDateTime judgeStartedAt) {
        this.judgeStartedAt = judgeStartedAt;
    }

    public LocalDateTime getJudgedAt() {
        return judgedAt;
    }

    public void setJudgedAt(LocalDateTime judgedAt) {
        this.judgedAt = judgedAt;
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
