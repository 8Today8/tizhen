package com.tizhen.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 选手 LLM 凭证。密文只在服务内部使用，禁止进入任何后台响应或日志。
 */
@TableName("llm_credential")
public class LlmCredential {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String provider;

    private String model;

    private String apiKeyCiphertext;

    private String apiKeyLast4;

    private Integer encryptionKeyVersion;

    private String status;

    private LocalDateTime lastValidatedAt;

    private String lastValidationError;

    private LocalDateTime deletedAt;

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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKeyCiphertext() {
        return apiKeyCiphertext;
    }

    public void setApiKeyCiphertext(String apiKeyCiphertext) {
        this.apiKeyCiphertext = apiKeyCiphertext;
    }

    public String getApiKeyLast4() {
        return apiKeyLast4;
    }

    public void setApiKeyLast4(String apiKeyLast4) {
        this.apiKeyLast4 = apiKeyLast4;
    }

    public Integer getEncryptionKeyVersion() {
        return encryptionKeyVersion;
    }

    public void setEncryptionKeyVersion(Integer encryptionKeyVersion) {
        this.encryptionKeyVersion = encryptionKeyVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastValidatedAt() {
        return lastValidatedAt;
    }

    public void setLastValidatedAt(LocalDateTime lastValidatedAt) {
        this.lastValidatedAt = lastValidatedAt;
    }

    public String getLastValidationError() {
        return lastValidationError;
    }

    public void setLastValidationError(String lastValidationError) {
        this.lastValidationError = lastValidationError;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
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
