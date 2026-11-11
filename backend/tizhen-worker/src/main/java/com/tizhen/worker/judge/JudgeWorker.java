package com.tizhen.worker.judge;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.model.JudgeOutcome;
import com.tizhen.core.judge.service.JudgeResultService;
import com.tizhen.core.judge.service.SubmissionTaskService;
import com.tizhen.core.problem.entity.Problem;
import com.tizhen.core.problem.entity.TestData;
import com.tizhen.core.problem.mapper.ProblemMapper;
import com.tizhen.core.problem.mapper.TestDataMapper;
import com.tizhen.core.problem.model.ScoringRubricItem;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步判题与简答判分消费器。OJ 提交和简答题都通过数据库任务表领取，
 * 不引入消息队列；判题结果只由本进程回写。
 */
@Component
public class JudgeWorker {

    private static final Logger log = LoggerFactory.getLogger(JudgeWorker.class);

    private static final String TYPE_OJ = "oj";
    private static final String TYPE_SHORT_ANSWER = "short_answer";
    private static final String LANGUAGE_CPP17 = "cpp17";
    private static final String LANGUAGE_PYTHON310 = "python310";
    private static final int DEFAULT_TIME_LIMIT_MS = 1_000;
    private static final int DEFAULT_MEMORY_LIMIT_MB = 256;
    private static final BigDecimal PASS_SCORE = BigDecimal.valueOf(60);

    private final SubmissionTaskService taskService;
    private final JudgeResultService resultService;
    private final GoJudgeClient goJudgeClient;
    private final PlatformLlmClient llmClient;
    private final ProblemMapper problemMapper;
    private final TestDataMapper testDataMapper;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final AtomicInteger activeTasks = new AtomicInteger();
    private final int concurrency;
    private final int maxAttempts;
    private final long staleSeconds;

    public JudgeWorker(SubmissionTaskService taskService,
                       JudgeResultService resultService,
                       GoJudgeClient goJudgeClient,
                       PlatformLlmClient llmClient,
                       ProblemMapper problemMapper,
                       TestDataMapper testDataMapper,
                       ObjectMapper objectMapper,
                       @Value("${worker.judge-concurrency:4}") int concurrency,
                       @Value("${tizhen.judge.max-attempts:3}") int maxAttempts,
                       @Value("${tizhen.judge.stale-seconds:300}") long staleSeconds) {
        this.taskService = taskService;
        this.resultService = resultService;
        this.goJudgeClient = goJudgeClient;
        this.llmClient = llmClient;
        this.problemMapper = problemMapper;
        this.testDataMapper = testDataMapper;
        this.objectMapper = objectMapper;
        this.concurrency = Math.max(concurrency, 1);
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.staleSeconds = Math.max(staleSeconds, 30);
        this.executor = Executors.newFixedThreadPool(this.concurrency, threadFactory());
    }

    @Scheduled(fixedDelayString = "${worker.poll-interval-ms:500}")
    public void poll() {
        int freeSlots = concurrency - activeTasks.get();
        if (freeSlots <= 0) {
            return;
        }
        try {
            List<Submission> ojTasks = taskService.claimOjTasks(freeSlots);
            dispatch(ojTasks, this::judgeOj);
            freeSlots -= ojTasks.size();
            if (freeSlots > 0) {
                List<Submission> shortAnswerTasks = taskService.claimShortAnswerTasks(freeSlots);
                dispatch(shortAnswerTasks, this::gradeShortAnswer);
            }
        } catch (RuntimeException exception) {
            log.error("领取判题任务失败", exception);
        }
    }

    @Scheduled(fixedDelayString = "${worker.recovery-interval-ms:30000}")
    public void recoverStaleTasks() {
        try {
            LocalDateTime threshold = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(staleSeconds);
            taskService.recoverStale(threshold, maxAttempts);
        } catch (RuntimeException exception) {
            log.error("恢复超时判题任务失败", exception);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void dispatch(List<Submission> tasks, java.util.function.Consumer<Submission> handler) {
        for (Submission task : tasks) {
            activeTasks.incrementAndGet();
            try {
                executor.submit(() -> {
                    try {
                        handler.accept(task);
                    } finally {
                        activeTasks.decrementAndGet();
                    }
                });
            } catch (RuntimeException exception) {
                activeTasks.decrementAndGet();
                log.error("提交判题任务失败: submissionId={}", task.getId(), exception);
            }
        }
    }

    private void judgeOj(Submission submission) {
        try {
            Problem problem = problemMapper.selectById(submission.getProblemId());
            if (problem == null || problem.getDeletedAt() != null
                    || !"published".equals(problem.getStatus())
                    || !TYPE_OJ.equals(problem.getType())) {
                resultService.complete(submission.getId(),
                        JudgeOutcome.failed("题目不可用，无法判题"));
                return;
            }
            String language = submission.getLanguage();
            if (!LANGUAGE_CPP17.equals(language) && !LANGUAGE_PYTHON310.equals(language)) {
                resultService.complete(submission.getId(),
                        JudgeOutcome.failed("提交语言不受支持"));
                return;
            }
            List<TestData> testData = testDataMapper.selectList(Wrappers.<TestData>lambdaQuery()
                    .eq(TestData::getProblemId, problem.getId())
                    .orderByAsc(TestData::getSortOrder)
                    .orderByAsc(TestData::getId));
            if (testData.isEmpty()) {
                resultService.complete(submission.getId(),
                        JudgeOutcome.failed("题目测试数据缺失，无法判题"));
                return;
            }

            int timeLimitMs = problem.getTimeLimitMs() == null
                    ? DEFAULT_TIME_LIMIT_MS
                    : problem.getTimeLimitMs();
            int memoryLimitMb = problem.getMemoryLimitMb() == null
                    ? DEFAULT_MEMORY_LIMIT_MB
                    : problem.getMemoryLimitMb();

            GoJudgeClient.CompileResult compile =
                    goJudgeClient.compile(language, submission.getCode());
            if (!compile.success()) {
                resultService.complete(submission.getId(), JudgeOutcome.finished(
                        "CE", null, "编译失败", compile.compileOutput(), null, null));
                return;
            }

            GoJudgeClient.CompiledProgram program = compile.program();
            try {
                runTests(submission, program, testData, timeLimitMs, memoryLimitMb);
            } finally {
                goJudgeClient.cleanup(program);
            }
        } catch (GoJudgeClient.GoJudgeException exception) {
            log.warn("判题沙箱不可用: submissionId={}, reason={}",
                    submission.getId(), exception.getMessage());
            resultService.complete(submission.getId(),
                    JudgeOutcome.failed("判题服务暂不可用，请稍后重新提交"));
        } catch (RuntimeException exception) {
            log.error("判题异常: submissionId={}", submission.getId(), exception);
            resultService.complete(submission.getId(),
                    JudgeOutcome.failed("判题失败，请稍后重新提交"));
        }
    }

    private void runTests(Submission submission,
                          GoJudgeClient.CompiledProgram program,
                          List<TestData> testData,
                          int timeLimitMs,
                          int memoryLimitMb) {
        long maxTimeMs = 0;
        long maxMemoryKb = 0;
        int index = 0;
        for (TestData test : testData) {
            index++;
            GoJudgeClient.RunResult run = goJudgeClient.runTest(program,
                    test.getInputData() == null ? "" : test.getInputData(),
                    timeLimitMs, memoryLimitMb);
            maxTimeMs = Math.max(maxTimeMs, run.timeMs());
            maxMemoryKb = Math.max(maxMemoryKb, run.memoryKb());

            if (!"Accepted".equals(run.status())) {
                String verdict = mapVerdict(run.status());
                if (verdict == null) {
                    throw new GoJudgeClient.GoJudgeException(
                            "未知沙箱状态: " + run.status());
                }
                resultService.complete(submission.getId(), JudgeOutcome.finished(
                        verdict, null, "测试点 " + index + " 未通过", null,
                        toInteger(maxTimeMs), toInteger(maxMemoryKb)));
                return;
            }
            if (!normalizeOutput(run.stdout()).equals(normalizeOutput(test.getOutputData()))) {
                resultService.complete(submission.getId(), JudgeOutcome.finished(
                        "WA", null, "测试点 " + index + " 答案错误", null,
                        toInteger(maxTimeMs), toInteger(maxMemoryKb)));
                return;
            }
        }
        resultService.complete(submission.getId(), JudgeOutcome.finished(
                "AC", null, "全部 " + testData.size() + " 个测试点通过", null,
                toInteger(maxTimeMs), toInteger(maxMemoryKb)));
    }

    private void gradeShortAnswer(Submission submission) {
        try {
            Problem problem = problemMapper.selectById(submission.getProblemId());
            if (problem == null || problem.getDeletedAt() != null
                    || !"published".equals(problem.getStatus())
                    || !TYPE_SHORT_ANSWER.equals(problem.getType())) {
                resultService.complete(submission.getId(),
                        JudgeOutcome.failed("题目不可用，无法判分"));
                return;
            }
            if (problem.getReferenceAnswer() == null
                    || problem.getReferenceAnswer().isBlank()) {
                resultService.complete(submission.getId(),
                        JudgeOutcome.failed("题目评分标准不完整"));
                return;
            }

            PlatformLlmClient.GradingResult grading = llmClient.grade(
                    problem.getTitle(),
                    problem.getStatement(),
                    problem.getReferenceAnswer(),
                    parseRubric(problem.getScoringRubric()),
                    submission.getAnswerText());
            String verdict = grading.score().compareTo(PASS_SCORE) >= 0 ? "AC" : "WA";
            resultService.complete(submission.getId(), JudgeOutcome.graded(
                    grading.score(), verdict, grading.feedback(),
                    "AI 评分 " + formatScore(grading.score())));
        } catch (PlatformLlmClient.LlmGradingException exception) {
            log.warn("简答判分失败: submissionId={}, reason={}",
                    submission.getId(), exception.getMessage());
            resultService.complete(submission.getId(),
                    JudgeOutcome.failed("简答判分服务暂不可用，请稍后重新提交"));
        } catch (RuntimeException exception) {
            log.error("简答判分异常: submissionId={}", submission.getId(), exception);
            resultService.complete(submission.getId(),
                    JudgeOutcome.failed("简答判分失败，请稍后重新提交"));
        }
    }

    private List<ScoringRubricItem> parseRubric(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<ScoringRubricItem>>() {
            });
        } catch (Exception exception) {
            log.warn("题目评分要点 JSON 解析失败");
            return List.of();
        }
    }

    private String mapVerdict(String status) {
        return switch (status) {
            case "Time Limit Exceeded" -> "TLE";
            case "Memory Limit Exceeded" -> "MLE";
            case "Nonzero Exit Status", "Signalled", "Dangerous Syscall" -> "RE";
            case "Output Limit Exceeded" -> "WA";
            default -> null;
        };
    }

    /**
     * 比较输出时忽略行尾空格与末尾空行，符合常见 OJ 判定口径。
     */
    private String normalizeOutput(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        int end = lines.length;
        while (end > 0 && lines[end - 1].trim().isEmpty()) {
            end--;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < end; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(stripTrailingWhitespace(lines[i]));
        }
        return builder.toString();
    }

    private String stripTrailingWhitespace(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(0, end);
    }

    private Integer toInteger(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    private String formatScore(BigDecimal score) {
        BigDecimal normalized = score.stripTrailingZeros();
        return normalized.scale() <= 0
                ? normalized.toBigInteger().toString()
                : normalized.toPlainString();
    }

    private ThreadFactory threadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "judge-worker-" + counter.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
    }
}
