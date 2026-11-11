package com.tizhen.core.llm;

import com.tizhen.common.exception.BusinessException;
import com.tizhen.common.exception.ErrorCode;
import com.tizhen.core.account.model.LlmProviderView;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 预置服务商注册表。地址与模型来自服务端配置，缺失时使用内置默认值，
 * 但客户端始终无法传入自定义地址。
 */
@Component
public class LlmProviderRegistry {

    private static final Map<String, String> NAMES = Map.of(
            "deepseek", "DeepSeek",
            "qwen", "通义千问",
            "zhipu", "智谱 GLM",
            "openai", "OpenAI");

    private static final Map<String, ProviderDefaults> DEFAULTS = Map.of(
            "deepseek", new ProviderDefaults(
                    "https://api.deepseek.com/v1",
                    List.of("deepseek-chat", "deepseek-reasoner"),
                    "deepseek-chat"),
            "qwen", new ProviderDefaults(
                    "https://dashscope.aliyuncs.com/compatible-mode/v1",
                    List.of("qwen-plus", "qwen-turbo", "qwen-max"),
                    "qwen-plus"),
            "zhipu", new ProviderDefaults(
                    "https://open.bigmodel.cn/api/paas/v4",
                    List.of("glm-4-plus", "glm-4-air", "glm-4-flash"),
                    "glm-4-plus"),
            "openai", new ProviderDefaults(
                    "https://api.openai.com/v1",
                    List.of("gpt-4o-mini", "gpt-4o"),
                    "gpt-4o-mini"));

    private static final List<String> ORDER = List.of("deepseek", "qwen", "zhipu", "openai");

    private final LlmProperties properties;

    public LlmProviderRegistry(LlmProperties properties) {
        this.properties = properties;
    }

    public List<LlmProviderView> list() {
        return ORDER.stream()
                .map(this::resolve)
                .map(provider -> new LlmProviderView(
                        provider.code(),
                        provider.name(),
                        provider.defaultModel(),
                        provider.models()))
                .toList();
    }

    public Provider resolve(String code) {
        String normalized = code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
        if (!ORDER.contains(normalized)) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_UNSUPPORTED);
        }
        ProviderDefaults defaults = DEFAULTS.get(normalized);
        LlmProperties.Provider configured = properties.getProviders().get(normalized);
        String baseUrl = configured == null ? null : configured.getBaseUrl();
        List<String> models = configured == null ? null : configured.getModels();
        String defaultModel = configured == null ? null : configured.getDefaultModel();
        return new Provider(
                normalized,
                NAMES.get(normalized),
                baseUrl == null || baseUrl.isBlank() ? defaults.baseUrl() : trimTrailingSlash(baseUrl),
                models == null || models.isEmpty() ? defaults.models() : List.copyOf(models),
                defaultModel == null || defaultModel.isBlank() ? defaults.defaultModel() : defaultModel);
    }

    public String displayName(String code) {
        try {
            return resolve(code).name();
        } catch (BusinessException exception) {
            return code;
        }
    }

    private String trimTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public record Provider(
            String code,
            String name,
            String baseUrl,
            List<String> models,
            String defaultModel
    ) {
    }

    private record ProviderDefaults(String baseUrl, List<String> models, String defaultModel) {
    }
}
