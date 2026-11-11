package com.tizhen.core.account.model;

import java.time.Instant;

/**
 * 选手 LLM 凭证脱敏视图。永不包含明文或密文。
 */
public record LlmCredentialView(
        boolean configured,
        String provider,
        String providerName,
        String model,
        String apiKeyMasked,
        String status,
        Instant lastValidatedAt,
        String lastValidationError,
        boolean hasActiveTask
) {
}
