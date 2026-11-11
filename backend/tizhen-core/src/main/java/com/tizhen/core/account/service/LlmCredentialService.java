package com.tizhen.core.account.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.common.util.TimeUtils;
import com.tizhen.core.account.entity.LlmCredential;
import com.tizhen.core.account.mapper.LlmCredentialMapper;
import com.tizhen.core.account.model.LlmCredentialView;
import com.tizhen.core.account.model.LlmProviderView;
import com.tizhen.core.datagen.entity.DataGenTask;
import com.tizhen.core.datagen.mapper.DataGenTaskMapper;
import com.tizhen.core.llm.LlmCredentialCipher;
import com.tizhen.core.llm.LlmProviderRegistry;
import com.tizhen.core.llm.LlmProviderRegistry.Provider;
import com.tizhen.core.llm.UserLlmClient;
import com.tizhen.core.llm.UserLlmClient.LlmCallException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 选手自带 LLM 凭证。明文集只在本服务内短暂使用；接口只返回脱敏信息。
 */
@Service
public class LlmCredentialService {

    private static final Set<String> ACTIVE_STATUSES = Set.of(
            "pending", "llm_generating", "llm_data_generating", "generating");
    private static final String STATUS_VALID = "valid";
    private static final String STATUS_INVALID = "invalid";
    private static final String STATUS_DELETED = "deleted";

    private final LlmCredentialMapper llmCredentialMapper;
    private final DataGenTaskMapper dataGenTaskMapper;
    private final LlmProviderRegistry providerRegistry;
    private final LlmCredentialCipher cipher;
    private final UserLlmClient userLlmClient;

    public LlmCredentialService(LlmCredentialMapper llmCredentialMapper,
                                DataGenTaskMapper dataGenTaskMapper,
                                LlmProviderRegistry providerRegistry,
                                LlmCredentialCipher cipher,
                                UserLlmClient userLlmClient) {
        this.llmCredentialMapper = llmCredentialMapper;
        this.dataGenTaskMapper = dataGenTaskMapper;
        this.providerRegistry = providerRegistry;
        this.cipher = cipher;
        this.userLlmClient = userLlmClient;
    }

    public List<LlmProviderView> providers() {
        return providerRegistry.list();
    }

    @Transactional(readOnly = true)
    public LlmCredentialView view(Long userId) {
        LlmCredential credential = findByUserId(userId);
        boolean hasActiveTask = hasActiveTask(userId);
        if (credential == null || STATUS_DELETED.equals(credential.getStatus())) {
            return new LlmCredentialView(false, null, null, null, null, null,
                    null, null, hasActiveTask);
        }
        return new LlmCredentialView(
                true,
                credential.getProvider(),
                providerRegistry.displayName(credential.getProvider()),
                credential.getModel(),
                maskedKey(credential.getApiKeyLast4()),
                credential.getStatus(),
                TimeUtils.toInstant(credential.getLastValidatedAt()),
                credential.getLastValidationError(),
                hasActiveTask);
    }

    @Transactional
    public LlmCredentialView save(Long userId, String providerCode, String apiKey, String model) {
        if (hasActiveTask(userId)) {
            throw new BusinessException(ErrorCode.LLM_CREDENTIAL_IN_USE);
        }
        Provider provider = providerRegistry.resolve(providerCode);
        String normalizedKey = apiKey == null ? "" : apiKey.trim();
        if (normalizedKey.length() < 8 || normalizedKey.length() > 512) {
            throw new BusinessException(ErrorCode.LLM_CREDENTIAL_INVALID, "API Key 长度不合法");
        }
        String resolvedModel = model == null || model.isBlank()
                ? provider.defaultModel()
                : model.trim();
        if (!provider.models().contains(resolvedModel)) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_PARAM, "模型不在服务商允许列表中");
        }
        if (!cipher.isConfigured()) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "LLM 凭证服务未配置");
        }

        try {
            userLlmClient.validate(provider, normalizedKey, resolvedModel);
        } catch (LlmCallException exception) {
            throw new BusinessException(ErrorCode.LLM_CREDENTIAL_INVALID, exception.getMessage());
        }

        String encrypted = cipher.encrypt(normalizedKey);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LlmCredential credential = findByUserId(userId);
        if (credential == null) {
            credential = new LlmCredential();
            credential.setUserId(userId);
            credential.setCreatedAt(now);
        }
        credential.setProvider(provider.code());
        credential.setModel(resolvedModel);
        credential.setApiKeyCiphertext(encrypted);
        credential.setApiKeyLast4(lastFour(normalizedKey));
        credential.setEncryptionKeyVersion(cipher.keyVersion());
        credential.setStatus(STATUS_VALID);
        credential.setLastValidatedAt(now);
        credential.setLastValidationError(null);
        credential.setDeletedAt(null);
        credential.setUpdatedAt(now);
        if (credential.getId() == null) {
            llmCredentialMapper.insert(credential);
        } else {
            llmCredentialMapper.updateById(credential);
        }
        return view(userId);
    }

    @Transactional
    public void delete(Long userId) {
        if (hasActiveTask(userId)) {
            throw new BusinessException(ErrorCode.LLM_CREDENTIAL_IN_USE);
        }
        LlmCredential credential = findByUserId(userId);
        if (credential == null || STATUS_DELETED.equals(credential.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        credential.setStatus(STATUS_DELETED);
        credential.setApiKeyCiphertext(null);
        credential.setApiKeyLast4(null);
        credential.setDeletedAt(now);
        credential.setLastValidationError(null);
        credential.setUpdatedAt(now);
        llmCredentialMapper.updateById(credential);
    }

    /**
     * 创建 LLM 任务前复检凭证。返回的实例仅供内部读取密文。
     */
    @Transactional(readOnly = true)
    public LlmCredential requireUsableCredential(Long userId) {
        LlmCredential credential = findByUserId(userId);
        if (credential == null
                || STATUS_DELETED.equals(credential.getStatus())
                || !STATUS_VALID.equals(credential.getStatus())
                || credential.getApiKeyCiphertext() == null) {
            throw new BusinessException(ErrorCode.LLM_CREDENTIAL_NOT_CONFIGURED);
        }
        Provider provider = providerRegistry.resolve(credential.getProvider());
        String plainKey = decrypt(credential);
        try {
            userLlmClient.validate(provider, plainKey, credential.getModel());
        } catch (LlmCallException exception) {
            markInvalid(credential.getId(), exception.getMessage());
            throw new BusinessException(ErrorCode.LLM_CREDENTIAL_NOT_CONFIGURED,
                    "LLM 凭证校验失败：" + exception.getMessage());
        }
        return credential;
    }

    /**
     * worker 读取任务快照中的凭证，校验归属与状态后返回密文行。
     */
    @Transactional(readOnly = true)
    public LlmCredential requireForTask(Long credentialId, Long userId) {
        LlmCredential credential = credentialId == null ? null
                : llmCredentialMapper.selectById(credentialId);
        if (credential == null
                || !userId.equals(credential.getUserId())
                || STATUS_DELETED.equals(credential.getStatus())
                || credential.getApiKeyCiphertext() == null) {
            throw new BusinessException(ErrorCode.LLM_CREDENTIAL_NOT_CONFIGURED);
        }
        return credential;
    }

    public String decrypt(LlmCredential credential) {
        try {
            return cipher.decrypt(credential.getApiKeyCiphertext());
        } catch (LlmCredentialCipher.CredentialCipherException exception) {
            throw new BusinessException(ErrorCode.LLM_CREDENTIAL_NOT_CONFIGURED,
                    "LLM 凭证无法解密，请重新配置");
        }
    }

    public Provider providerOf(LlmCredential credential) {
        return providerRegistry.resolve(credential.getProvider());
    }

    @Transactional
    public void markInvalid(Long credentialId, String reason) {
        LlmCredential credential = llmCredentialMapper.selectById(credentialId);
        if (credential == null || STATUS_DELETED.equals(credential.getStatus())) {
            return;
        }
        credential.setStatus(STATUS_INVALID);
        credential.setLastValidationError(truncateReason(reason));
        credential.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        llmCredentialMapper.updateById(credential);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveTask(Long userId) {
        Long count = dataGenTaskMapper.selectCount(Wrappers.<DataGenTask>lambdaQuery()
                .eq(DataGenTask::getUserId, userId)
                .ne(DataGenTask::getGenerationMode, "random")
                .in(DataGenTask::getStatus, ACTIVE_STATUSES));
        return count != null && count > 0;
    }

    private LlmCredential findByUserId(Long userId) {
        return llmCredentialMapper.selectOne(Wrappers.<LlmCredential>lambdaQuery()
                .eq(LlmCredential::getUserId, userId)
                .last("LIMIT 1"));
    }

    private String maskedKey(String last4) {
        return last4 == null || last4.isBlank() ? null : "****" + last4;
    }

    private String lastFour(String key) {
        String trimmed = key.trim();
        return trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
    }

    private String truncateReason(String reason) {
        String normalized = reason == null ? "API Key 校验失败" : reason;
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
