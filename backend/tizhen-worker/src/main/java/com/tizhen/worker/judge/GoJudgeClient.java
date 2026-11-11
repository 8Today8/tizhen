package com.tizhen.worker.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * go-judge 执行器客户端。worker 是唯一调用方，用户代码只进入沙箱，
 * 不接触业务数据卷与网络。
 */
@Component
public class GoJudgeClient {

    private static final Logger log = LoggerFactory.getLogger(GoJudgeClient.class);

    private static final String CPP_COMPILER = "/usr/bin/g++";
    private static final String PYTHON_RUNNER = "/usr/bin/python3";
    private static final String PYTHON_SYNTAX_CHECK =
            "import ast;ast.parse(open('main.py',encoding='utf-8').read())";
    private static final int PROC_LIMIT = 64;
    private static final long COMPILE_MEMORY_BYTES = 512L * 1024 * 1024;
    private static final long COMPILE_STACK_BYTES = 64L * 1024 * 1024;
    private static final String LANGUAGE_CPP17 = "cpp17";
    private static final String LANGUAGE_PYTHON310 = "python310";
    // 沙箱不会自动继承宿主 PATH，g++ 链接阶段需要用它查找 ld。
    private static final String SANDBOX_PATH =
            "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final int compileTimeoutMs;
    private final int maxOutputBytes;

    public GoJudgeClient(ObjectMapper objectMapper,
                         @Value("${tizhen.judge.base-url:http://127.0.0.1:5050}") String baseUrl,
                         @Value("${tizhen.judge.compile-timeout-ms:20000}") int compileTimeoutMs,
                         @Value("${tizhen.judge.max-output-bytes:16777216}") int maxOutputBytes) {
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.compileTimeoutMs = compileTimeoutMs;
        this.maxOutputBytes = maxOutputBytes;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public CompileResult compile(String language, String source) {
        if (LANGUAGE_CPP17.equals(language)) {
            return compileCpp(source);
        }
        if (LANGUAGE_PYTHON310.equals(language)) {
            return compilePython(source);
        }
        return new CompileResult(false, "不支持的语言", null);
    }

    public RunResult runTest(CompiledProgram program, String input, int timeLimitMs, int memoryLimitMb) {
        ObjectNode cmd = objectMapper.createObjectNode();
        ArrayNode args = cmd.putArray("args");
        if (LANGUAGE_CPP17.equals(program.language())) {
            args.add("main");
        } else {
            args.add(PYTHON_RUNNER).add("main.py");
        }
        cmd.putArray("env").add("PATH=" + SANDBOX_PATH);
        cmd.set("files", runtimeFiles(input));

        ObjectNode copyIn = objectMapper.createObjectNode();
        ObjectNode sourceFile = copyIn.putObject("main.py");
        if (LANGUAGE_CPP17.equals(program.language())) {
            ObjectNode binary = copyIn.putObject("main");
            binary.put("fileId", program.cachedFileId());
            copyIn.remove("main.py");
        } else {
            sourceFile.put("content", program.source());
        }
        cmd.set("copyIn", copyIn);

        applyLimits(cmd, timeLimitMs, memoryLimitMb);
        return execute(cmd, timeLimitMs + 5_000L);
    }

    public void cleanup(CompiledProgram program) {
        if (program == null || program.cachedFileId() == null
                || program.cachedFileId().isBlank()) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/file/" + program.cachedFileId()))
                    .timeout(Duration.ofSeconds(5))
                    .DELETE()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception exception) {
            // 缓存文件清理失败不影响判题结果，由 go-judge 自身生命周期兜底。
            log.warn("清理 go-judge 缓存文件失败: {}", exception.getMessage());
        }
    }

    private CompileResult compileCpp(String source) {
        ObjectNode cmd = objectMapper.createObjectNode();
        cmd.putArray("args")
                .add(CPP_COMPILER)
                .add("main.cpp")
                .add("-O2")
                .add("-std=c++17")
                .add("-o")
                .add("main");
        cmd.putArray("env").add("PATH=" + SANDBOX_PATH);
        cmd.set("files", emptyStdinWithCollectors());

        ObjectNode copyIn = objectMapper.createObjectNode();
        copyIn.putObject("main.cpp").put("content", source);
        cmd.set("copyIn", copyIn);
        cmd.putArray("copyOutCached").add("main");

        cmd.put("cpuLimit", millisToNanos(compileTimeoutMs));
        cmd.put("clockLimit", millisToNanos(compileTimeoutMs + 5_000L));
        cmd.put("memoryLimit", COMPILE_MEMORY_BYTES);
        cmd.put("stackLimit", COMPILE_STACK_BYTES);
        cmd.put("procLimit", PROC_LIMIT);

        JsonNode result = executeRaw(cmd, compileTimeoutMs + 10_000L);
        String output = collectOutput(result);
        if (!isAccepted(result)) {
            return new CompileResult(false, output, null);
        }
        JsonNode fileIds = result.path("fileIds");
        String fileId = fileIds.path("main").asText("");
        if (fileId.isBlank()) {
            return new CompileResult(false, output.isBlank() ? "编译产物缺失" : output, null);
        }
        return new CompileResult(true, output,
                new CompiledProgram(LANGUAGE_CPP17, source, fileId));
    }

    private CompileResult compilePython(String source) {
        ObjectNode cmd = objectMapper.createObjectNode();
        cmd.putArray("args")
                .add(PYTHON_RUNNER)
                .add("-c")
                .add(PYTHON_SYNTAX_CHECK);
        cmd.putArray("env").add("PATH=" + SANDBOX_PATH);
        cmd.set("files", emptyStdinWithCollectors());

        ObjectNode copyIn = objectMapper.createObjectNode();
        copyIn.putObject("main.py").put("content", source);
        cmd.set("copyIn", copyIn);

        cmd.put("cpuLimit", millisToNanos(compileTimeoutMs));
        cmd.put("clockLimit", millisToNanos(compileTimeoutMs + 5_000L));
        cmd.put("memoryLimit", COMPILE_MEMORY_BYTES);
        cmd.put("stackLimit", COMPILE_STACK_BYTES);
        cmd.put("procLimit", PROC_LIMIT);

        JsonNode result = executeRaw(cmd, compileTimeoutMs + 10_000L);
        String output = collectOutput(result);
        if (!isAccepted(result)) {
            return new CompileResult(false, output, null);
        }
        return new CompileResult(true, output,
                new CompiledProgram(LANGUAGE_PYTHON310, source, null));
    }

    private RunResult execute(ObjectNode cmd, long timeoutMs) {
        JsonNode result = executeRaw(cmd, timeoutMs);
        String status = result.path("status").asText("Internal Error");
        String error = result.path("error").asText("");
        if ("Internal Error".equals(status) || "File Error".equals(status)) {
            throw new GoJudgeException(error.isBlank() ? "go-judge 执行异常" : error);
        }
        long timeNanos = result.path("time").asLong(0);
        long memoryBytes = result.path("memory").asLong(0);
        return new RunResult(
                status,
                result.path("exitStatus").asInt(-1),
                timeNanos <= 0 ? 0 : (timeNanos + 999_999) / 1_000_000,
                memoryBytes <= 0 ? 0 : memoryBytes / 1024,
                result.path("files").path("stdout").asText(""),
                result.path("files").path("stderr").asText(""),
                error);
    }

    private JsonNode executeRaw(ObjectNode cmd, long timeoutMs) {
        ObjectNode body = objectMapper.createObjectNode();
        body.putArray("cmd").add(cmd);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/run"))
                    .timeout(Duration.ofMillis(Math.max(timeoutMs, 1_000L)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new GoJudgeException("go-judge 返回 HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String requestError = root.path("error").asText("");
            // go-judge 的 /run 正常返回结果数组；部分错误响应才是带 error 的对象。
            JsonNode results = root.isArray() ? root : root.path("results");
            if (!results.isArray() || results.isEmpty()) {
                throw new GoJudgeException(requestError.isBlank()
                        ? "go-judge 未返回执行结果"
                        : requestError);
            }
            return results.get(0);
        } catch (IOException exception) {
            throw new GoJudgeException("无法连接判题沙箱", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GoJudgeException("判题请求被中断", exception);
        }
    }

    private ArrayNode emptyStdinWithCollectors() {
        return runtimeFiles("");
    }

    private ArrayNode runtimeFiles(String input) {
        ArrayNode files = objectMapper.createArrayNode();
        files.addObject().put("content", input);
        files.addObject().put("name", "stdout").put("max", maxOutputBytes);
        files.addObject().put("name", "stderr").put("max", maxOutputBytes);
        return files;
    }

    private void applyLimits(ObjectNode cmd, int timeLimitMs, int memoryLimitMb) {
        long cpuNanos = millisToNanos(timeLimitMs);
        long clockMs = Math.max(timeLimitMs * 2L, timeLimitMs + 1_000L);
        cmd.put("cpuLimit", cpuNanos);
        cmd.put("clockLimit", millisToNanos(clockMs));
        cmd.put("memoryLimit", (long) memoryLimitMb * 1024 * 1024);
        cmd.put("stackLimit", (long) memoryLimitMb * 1024 * 1024);
        cmd.put("procLimit", PROC_LIMIT);
    }

    private long millisToNanos(long millis) {
        return millis * 1_000_000L;
    }

    private boolean isAccepted(JsonNode result) {
        return "Accepted".equals(result.path("status").asText())
                && result.path("exitStatus").asInt(1) == 0;
    }

    private String collectOutput(JsonNode result) {
        String stdout = result.path("files").path("stdout").asText("");
        String stderr = result.path("files").path("stderr").asText("");
        String error = result.path("error").asText("");
        String combined = (stdout + stderr + (error.isBlank() ? "" : error)).trim();
        if (combined.length() > 8_000) {
            return combined.substring(0, 8_000);
        }
        return combined;
    }

    private String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public record CompileResult(boolean success, String compileOutput, CompiledProgram program) {
    }

    public record CompiledProgram(String language, String source, String cachedFileId) {
    }

    public record RunResult(
            String status,
            int exitStatus,
            long timeMs,
            long memoryKb,
            String stdout,
            String stderr,
            String error
    ) {
    }

    public static class GoJudgeException extends RuntimeException {
        public GoJudgeException(String message) {
            super(message);
        }

        public GoJudgeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
