package com.tizhen.core.infra.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.common.util.WeekUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Codeforces 公共 API 适配器：读取选手提交记录，计算历史与本周过题数。
 * Codeforces 限制接口调用频率，所有请求在进程内串行并保持最小间隔。
 */
@Component
public class CodeforcesClient {

    private static final Logger log = LoggerFactory.getLogger(CodeforcesClient.class);
    private static final String STATUS_OK = "OK";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final long minRequestIntervalMillis;
    private final Object rateLimitLock = new Object();
    private long lastRequestNanos;

    public CodeforcesClient(
            ObjectMapper objectMapper,
            @Value("${tizhen.codeforces.base-url:https://codeforces.com}") String baseUrl,
            @Value("${tizhen.codeforces.connect-timeout-seconds:5}") long connectTimeoutSeconds,
            @Value("${tizhen.codeforces.request-timeout-seconds:30}") long requestTimeoutSeconds,
            @Value("${tizhen.codeforces.min-request-interval-millis:2100}") long minRequestIntervalMillis) {
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.requestTimeout = Duration.ofSeconds(Math.max(requestTimeoutSeconds, 5));
        this.minRequestIntervalMillis = Math.max(minRequestIntervalMillis, 0);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(connectTimeoutSeconds, 1)))
                .build();
    }

    /**
     * 拉取指定 Handle 的过题统计。Handle 不存在时抛出 {@link HandleNotFoundException}，
     * 网络或 Codeforces 服务异常时抛出 {@link UnavailableException}。
     */
    public CodeforcesSolvedStats fetchSolvedStats(String handle) {
        JsonNode statusRoot = get("/api/user.status", handle);
        JsonNode submissions = requireResult(statusRoot);
        LocalDate weekStartDate = WeekUtils.currentWeekStart();
        if (!submissions.isArray() || submissions.isEmpty()) {
            // user.status 对无提交记录与不存在的 Handle 都返回空数组，再用 user.info 区分。
            ensureHandleExists(handle);
            return new CodeforcesSolvedStats(0, 0, weekStartDate);
        }

        Instant weekStart = weekStartDate.atStartOfDay(WeekUtils.BUSINESS_ZONE).toInstant();
        Instant weekEnd = weekStartDate.plusWeeks(1).atStartOfDay(WeekUtils.BUSINESS_ZONE).toInstant();
        Map<String, Long> firstAcceptedAt = new HashMap<>();
        for (JsonNode submission : submissions) {
            if (!"OK".equals(submission.path("verdict").asText(""))) {
                continue;
            }
            JsonNode problem = submission.path("problem");
            String problemKey = problemKey(problem);
            if (problemKey == null) {
                continue;
            }
            long acceptedAt = submission.path("creationTimeSeconds").asLong(0);
            if (acceptedAt <= 0) {
                continue;
            }
            firstAcceptedAt.merge(problemKey, acceptedAt, Math::min);
        }

        long weekStartEpoch = weekStart.getEpochSecond();
        long weekEndEpoch = weekEnd.getEpochSecond();
        int weeklySolved = 0;
        for (long acceptedAt : firstAcceptedAt.values()) {
            if (acceptedAt >= weekStartEpoch && acceptedAt < weekEndEpoch) {
                weeklySolved++;
            }
        }
        return new CodeforcesSolvedStats(firstAcceptedAt.size(), weeklySolved, weekStartDate);
    }

    private JsonNode get(String path, String handle) {
        awaitRateLimit();
        String encodedHandle = URLEncoder.encode(handle, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path + "?handle=" + encodedHandle))
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("User-Agent", "tizhen/0.1")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            log.warn("Codeforces 请求失败，path={}", path, exception);
            throw new UnavailableException("Codeforces 请求失败", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UnavailableException("Codeforces 请求被中断", exception);
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (root == null || root.isMissingNode()) {
                throw new UnavailableException("Codeforces 返回内容为空");
            }
            return root;
        } catch (IOException exception) {
            log.warn("Codeforces 返回内容无法解析，path={}，HTTP {}", path, response.statusCode());
            throw new UnavailableException("Codeforces 返回内容无法解析", exception);
        }
    }

    private JsonNode requireResult(JsonNode root) {
        String status = root.path("status").asText("");
        if (!STATUS_OK.equals(status)) {
            String comment = root.path("comment").asText("");
            if (comment.contains("not found")) {
                throw new HandleNotFoundException();
            }
            log.warn("Codeforces API 返回失败：{}", comment);
            throw new UnavailableException("Codeforces API 返回失败");
        }
        return root.path("result");
    }

    private void ensureHandleExists(String handle) {
        JsonNode root = get("/api/user.info", handle);
        String status = root.path("status").asText("");
        if (STATUS_OK.equals(status)) {
            return;
        }
        String comment = root.path("comment").asText("");
        if (comment.contains("not found")) {
            throw new HandleNotFoundException();
        }
        log.warn("Codeforces Handle 校验失败，handle={}，原因={}", handle, comment);
        throw new UnavailableException("Codeforces Handle 校验失败");
    }

    private String problemKey(JsonNode problem) {
        String index = problem.path("index").asText("");
        if (index.isEmpty()) {
            return null;
        }
        JsonNode contestId = problem.path("contestId");
        if (contestId.isNumber()) {
            return contestId.asInt() + ":" + index;
        }
        String problemsetName = problem.path("problemsetName").asText("");
        String name = problem.path("name").asText("");
        if (problemsetName.isEmpty() && name.isEmpty()) {
            return null;
        }
        return problemsetName + ":" + index + ":" + name;
    }

    private void awaitRateLimit() {
        synchronized (rateLimitLock) {
            if (lastRequestNanos > 0) {
                long elapsedMillis = (System.nanoTime() - lastRequestNanos) / 1_000_000L;
                long waitMillis = minRequestIntervalMillis - elapsedMillis;
                if (waitMillis > 0) {
                    try {
                        Thread.sleep(waitMillis);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new UnavailableException("Codeforces 请求等待被中断", exception);
                    }
                }
            }
            lastRequestNanos = System.nanoTime();
        }
    }

    private String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * Codeforces Handle 不存在。
     */
    public static class HandleNotFoundException extends RuntimeException {
    }

    /**
     * Codeforces 服务不可用或返回异常。
     */
    public static class UnavailableException extends RuntimeException {
        public UnavailableException(String message) {
            super(message);
        }

        public UnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
