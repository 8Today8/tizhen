package com.tizhen.core.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 使用选手自带 API Key 调用 OpenAI Chat Completions 兼容接口。
 * Key 只作为请求头发送，不进入日志、异常或响应。
 */
@Component
public class UserLlmClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final LlmProperties properties;

    public UserLlmClient(ObjectMapper objectMapper, LlmProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 轻量校验：发送 1 token 的请求，确认 Key 可用。失败抛出带脱敏原因的异常。
     */
    public void validate(LlmProviderRegistry.Provider provider, String apiKey, String model) {
        chat(provider,
                apiKey,
                model,
                "你是连通性校验助手。",
                "只回复 ok。",
                1,
                Duration.ofSeconds(Math.max(properties.getValidationTimeoutSeconds(), 5)));
    }

    public String generate(LlmProviderRegistry.Provider provider,
                           String apiKey,
                           String model,
                           String systemPrompt,
                           String userPrompt,
                           int maxTokens) {
        return chat(provider,
                apiKey,
                model,
                systemPrompt,
                userPrompt,
                maxTokens,
                Duration.ofSeconds(Math.max(properties.getGenerationTimeoutSeconds(), 30)));
    }

    public String chat(LlmProviderRegistry.Provider provider,
                       String apiKey,
                       String model,
                       String systemPrompt,
                       String userPrompt,
                       int maxTokens,
                       Duration timeout) {
        String resolvedModel = model == null || model.isBlank() ? provider.defaultModel() : model;
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", resolvedModel);
        body.put("temperature", 0.2);
        body.put("max_tokens", Math.max(maxTokens, 1));
        body.put("stream", false);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(provider.baseUrl() + "/chat/completions"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw failure(response.statusCode(), response.body(), apiKey);
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = extractContent(root);
            if (content.isBlank()) {
                throw new LlmCallException("LLM 返回内容为空", false);
            }
            return content;
        } catch (IOException exception) {
            throw new LlmCallException("无法连接 LLM 服务商", false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmCallException("LLM 请求被中断", false);
        }
    }

    private String extractContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new LlmCallException("LLM 返回格式异常", false);
        }
        JsonNode content = choices.get(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText("");
        }
        if (content.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : content) {
                String text = item.path("text").asText("");
                if (!text.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(text);
                }
            }
            return builder.toString();
        }
        return content.asText("");
    }

    private LlmCallException failure(int status, String body, String apiKey) {
        String message = extractErrorMessage(body);
        if (apiKey != null && !apiKey.isBlank()) {
            message = message.replace(apiKey, "[redacted]");
        }
        if (status == 401 || status == 403) {
            return new LlmCallException("API Key 无效或已过期", true);
        }
        if (status == 429) {
            return new LlmCallException("服务商调用频率受限，请稍后重试", false);
        }
        String suffix = message.isBlank() ? "" : "：" + message;
        return new LlmCallException("LLM 服务商返回 HTTP " + status + suffix, false);
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String message = root.path("error").path("message").asText("");
            if (message.isBlank()) {
                message = root.path("message").asText("");
            }
            return truncate(message, 240);
        } catch (IOException exception) {
            return "";
        }
    }

    private String truncate(String value, int maxLength) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    public static class LlmCallException extends RuntimeException {

        private final boolean authFailure;

        public LlmCallException(String message, boolean authFailure) {
            super(message);
            this.authFailure = authFailure;
        }

        public boolean isAuthFailure() {
            return authFailure;
        }
    }
}
