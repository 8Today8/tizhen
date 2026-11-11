package com.tizhen.core.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 选手自带 LLM 凭证相关配置。服务商地址与模型列表由服务端维护，
 * 客户端不能提交自定义 Base URL。
 */
@ConfigurationProperties(prefix = "tizhen.llm")
public class LlmProperties {

    private String credentialEncryptionKey;

    private int credentialKeyVersion = 1;

    private long validationTimeoutSeconds = 15;

    private long generationTimeoutSeconds = 180;

    private Map<String, Provider> providers = new LinkedHashMap<>();

    public String getCredentialEncryptionKey() {
        return credentialEncryptionKey;
    }

    public void setCredentialEncryptionKey(String credentialEncryptionKey) {
        this.credentialEncryptionKey = credentialEncryptionKey;
    }

    public int getCredentialKeyVersion() {
        return credentialKeyVersion;
    }

    public void setCredentialKeyVersion(int credentialKeyVersion) {
        this.credentialKeyVersion = credentialKeyVersion;
    }

    public long getValidationTimeoutSeconds() {
        return validationTimeoutSeconds;
    }

    public void setValidationTimeoutSeconds(long validationTimeoutSeconds) {
        this.validationTimeoutSeconds = validationTimeoutSeconds;
    }

    public long getGenerationTimeoutSeconds() {
        return generationTimeoutSeconds;
    }

    public void setGenerationTimeoutSeconds(long generationTimeoutSeconds) {
        this.generationTimeoutSeconds = generationTimeoutSeconds;
    }

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers == null ? new LinkedHashMap<>() : providers;
    }

    public static class Provider {

        private String baseUrl;

        private List<String> models = List.of();

        private String defaultModel;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public List<String> getModels() {
            return models;
        }

        public void setModels(List<String> models) {
            this.models = models == null ? List.of() : models;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }
    }
}
