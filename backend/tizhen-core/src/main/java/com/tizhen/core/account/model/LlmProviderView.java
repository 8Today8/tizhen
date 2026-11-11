package com.tizhen.core.account.model;

import java.util.List;

/**
 * 预置 LLM 服务商与可选模型。
 */
public record LlmProviderView(
        String provider,
        String name,
        String defaultModel,
        List<String> models
) {
}
