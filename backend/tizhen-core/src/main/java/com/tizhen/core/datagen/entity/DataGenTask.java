package com.tizhen.core.datagen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * AI 造数据任务。实体刻意不包含 generatorCode，避免内部构造器代码被读取或序列化。
 */
@TableName("data_gen_task")
public class DataGenTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String statement;

    private String inputFormat;

    private String outputFormat;

    private String stdCode;

    private String stdLanguage;

    private String dataRange;

    private Integer groupCount;

    private String generationMode;

    private String specialRequirements;

    private String llmProvider;

    private String llmModel;

    private Long llmCredentialId;

    private String generatorCode;

    private String generatorStatus;

    private String llmDataStatus;

    private String status;

    private String failureStage;

    private String failureReason;

    private String resultSummary;

    private Integer timeLimitMs;

    private Integer memoryLimitMb;

    private Integer attempts;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime heartbeatAt;

    private LocalDateTime filesExpireAt;

    private LocalDateTime filesCleanedAt;

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

    public String getStdCode() {
        return stdCode;
    }

    public void setStdCode(String stdCode) {
        this.stdCode = stdCode;
    }

    public String getStdLanguage() {
        return stdLanguage;
    }

    public void setStdLanguage(String stdLanguage) {
        this.stdLanguage = stdLanguage;
    }

    public String getDataRange() {
        return dataRange;
    }

    public void setDataRange(String dataRange) {
        this.dataRange = dataRange;
    }

    public Integer getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(Integer groupCount) {
        this.groupCount = groupCount;
    }

    public String getGenerationMode() {
        return generationMode;
    }

    public void setGenerationMode(String generationMode) {
        this.generationMode = generationMode;
    }

    public String getSpecialRequirements() {
        return specialRequirements;
    }

    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }

    public Long getLlmCredentialId() {
        return llmCredentialId;
    }

    public void setLlmCredentialId(Long llmCredentialId) {
        this.llmCredentialId = llmCredentialId;
    }

    public String getGeneratorCode() {
        return generatorCode;
    }

    public void setGeneratorCode(String generatorCode) {
        this.generatorCode = generatorCode;
    }

    public String getGeneratorStatus() {
        return generatorStatus;
    }

    public void setGeneratorStatus(String generatorStatus) {
        this.generatorStatus = generatorStatus;
    }

    public String getLlmDataStatus() {
        return llmDataStatus;
    }

    public void setLlmDataStatus(String llmDataStatus) {
        this.llmDataStatus = llmDataStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureStage() {
        return failureStage;
    }

    public void setFailureStage(String failureStage) {
        this.failureStage = failureStage;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
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

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getFilesExpireAt() {
        return filesExpireAt;
    }

    public void setFilesExpireAt(LocalDateTime filesExpireAt) {
        this.filesExpireAt = filesExpireAt;
    }

    public LocalDateTime getFilesCleanedAt() {
        return filesCleanedAt;
    }

    public void setFilesCleanedAt(LocalDateTime filesCleanedAt) {
        this.filesCleanedAt = filesCleanedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getHeartbeatAt() {
        return heartbeatAt;
    }

    public void setHeartbeatAt(LocalDateTime heartbeatAt) {
        this.heartbeatAt = heartbeatAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
