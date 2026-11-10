package com.tizhen.core.account.model;

import java.time.Instant;

/**
 * 凭证配置状态。不包含 Key 明文、密文或末四位。
 */
public record AdminLlmCredentialView(
        boolean configured,
        String provider,
        String providerName,
        String model,
        String status,
        Instant lastValidatedAt,
        String lastValidationError
) {
}
