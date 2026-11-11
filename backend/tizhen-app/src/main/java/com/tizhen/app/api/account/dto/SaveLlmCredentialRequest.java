package com.tizhen.app.api.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新建或替换 LLM 凭证。apiKey 只进入服务端加密流程。
 */
public record SaveLlmCredentialRequest(
        @NotBlank(message = "服务商不能为空")
        String provider,

        @NotBlank(message = "API Key 不能为空")
        @Size(min = 8, max = 512, message = "API Key 长度不合法")
        String apiKey,

        @Size(max = 64, message = "模型名过长")
        String model
) {
}
