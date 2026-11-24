package com.tizhen.worker.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tizhen.core.account.service.LlmCredentialService;
import com.tizhen.core.battle.mapper.BattleRoomMapper;
import com.tizhen.core.battle.service.BattleLifecycleService;
import com.tizhen.core.datagen.entity.DataGenFile;
import com.tizhen.core.datagen.entity.DataGenTask;
import com.tizhen.core.datagen.mapper.DataGenFileMapper;
import com.tizhen.core.datagen.mapper.DataGenTaskMapper;
import com.tizhen.core.infra.file.FileStorage;
import com.tizhen.core.infra.file.StoredFile;
import com.tizhen.core.judge.entity.FirstAc;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.entity.WrongBook;
import com.tizhen.core.judge.mapper.FirstAcMapper;
import com.tizhen.core.judge.mapper.SubmissionMapper;
import com.tizhen.core.judge.mapper.WrongBookMapper;
import com.tizhen.core.judge.service.JudgeResultService;
import com.tizhen.core.judge.service.SubmissionTaskService;
import com.tizhen.core.llm.LlmProviderRegistry;
import com.tizhen.core.llm.UserLlmClient;
import com.tizhen.core.mq.MqConfig;
import com.tizhen.core.mq.MqNames;
import com.tizhen.core.mq.TaskDispatchListener;
import com.tizhen.core.mq.TaskMessage;
import com.tizhen.core.mq.TaskPublisher;
import com.tizhen.core.mq.TaskType;
import com.tizhen.core.problem.entity.Problem;
import com.tizhen.core.problem.entity.TestData;
import com.tizhen.core.problem.mapper.ProblemMapper;
import com.tizhen.core.problem.mapper.TestDataMapper;
import com.tizhen.testsupport.MessagingFullStackTestBase;
import com.tizhen.worker.datagen.DataGenMqConsumer;
import com.tizhen.worker.datagen.DataGenTaskStateService;
import com.tizhen.worker.datagen.DataGenWorker;
import com.tizhen.worker.judge.GoJudgeClient;
import com.tizhen.worker.judge.JudgeMqConsumer;
import com.tizhen.worker.judge.JudgeWorker;
import com.tizhen.worker.judge.PlatformLlmClient;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 真实业务链路集成测试：使用真实 MySQL、RabbitMQ、Redis 与真实 worker 执行器，
 * 只把 go-judge 与平台 LLM 两个外部依赖替换为 Mock。
 * 覆盖三类任务的执行与回写、重复投递、重试耗尽进死信、超时恢复、补偿扫描与轻量负载。
 */
@SpringBootTest(classes = BusinessChainIntegrationTest.TestApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                "spring.rabbitmq.dynamic=false",
                "tizhen.mq.enabled=true",
                "tizhen.mq.retry-delays=1s",
                "tizhen.mq.stale-seconds=3",
                "tizhen.mq.scan-interval-ms=600000",
                "tizhen.judge.max-attempts=3",
                "worker.judge-concurrency=4",
                "worker.datagen-concurrency=2",
                "worker.datagen-max-attempts=2"
        })
class BusinessChainIntegrationTest extends MessagingFullStackTestBase {

    private static final int MAX_ATTEMPTS = 3;
    private static final String EXPECTED_OUTPUT = "42\n";

    @Autowired
    private TaskPublisher publisher;
    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private MessageConverter messageConverter;
    @Autowired
    private GoJudgeClient goJudgeClient;
    @Autowired
    private PlatformLlmClient llmClient;
    @Autowired
    private FileStorage fileStorage;
    @Autowired
    private SubmissionMapper submissionMapper;
    @Autowired
    private FirstAcMapper firstAcMapper;
    @Autowired
    private WrongBookMapper wrongBookMapper;
    @Autowired
    private ProblemMapper problemMapper;
    @Autowired
    private TestDataMapper testDataMapper;
    @Autowired
    private DataGenTaskMapper dataGenTaskMapper;
    @Autowired
    private DataGenFileMapper dataGenFileMapper;
    @Autowired
    private SubmissionTaskService taskService;
    @Autowired
    private TaskCompensationScanner compensationScanner;

    private final AtomicInteger fileSequence = new AtomicInteger();

    @BeforeEach
    void resetState() {
        Mockito.reset(goJudgeClient, llmClient, fileStorage);
        when(goJudgeClient.compile(anyString(), anyString())).thenReturn(
                new GoJudgeClient.CompileResult(true, "",
                        new GoJudgeClient.CompiledProgram("cpp17", "source", null)));
        when(goJudgeClient.runTest(any(), anyString(), anyInt(), anyInt())).thenReturn(
                new GoJudgeClient.RunResult("Accepted", 0, 1, 128,
                        EXPECTED_OUTPUT, "", ""));
        when(llmClient.grade(anyString(), anyString(), anyString(), anyList(), anyString()))
                .thenReturn(new PlatformLlmClient.GradingResult(
                        new BigDecimal("80"), "回答完整"));
        try {
            doAnswer(invocation ->
                    new StoredFile("datagen/file-" + fileSequence.incrementAndGet(), 0))
                    .when(fileStorage).store(any(InputStream.class), anyString());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        for (TaskType taskType : TaskType.values()) {
            amqpAdmin.purgeQueue(taskType.queue(), false);
            amqpAdmin.purgeQueue(taskType.deadLetterQueue(), false);
            amqpAdmin.purgeQueue(MqNames.retryQueue(taskType, Duration.ofSeconds(1)), false);
        }
    }

    @Test
    void ojSubmissionIsJudgedAndFirstAcRecorded() {
        Problem problem = insertOjProblem();
        Submission submission = insertOjSubmission(101L, problem.getId());

        publisher.publish(new TaskMessage(submission.getId(), TaskType.JUDGE_OJ));

        Submission finished = awaitSubmission(submission.getId(),
                Duration.ofSeconds(20), "finished");
        assertEquals("AC", finished.getVerdict());
        assertEquals(1, finished.getJudgeAttempts().intValue());
        assertEquals("全部 1 个测试点通过", finished.getJudgeDetail());
        FirstAc firstAc = firstAcMapper.selectOne(Wrappers.<FirstAc>lambdaQuery()
                .eq(FirstAc::getUserId, 101L)
                .eq(FirstAc::getProblemId, problem.getId()));
        assertNotNull(firstAc, "首次 AC 应写入 first_ac");
        assertEquals(submission.getId(), firstAc.getSubmissionId());
    }

    @Test
    void wrongAnswerIsWrittenToWrongBook() {
        when(goJudgeClient.runTest(any(), anyString(), anyInt(), anyInt())).thenReturn(
                new GoJudgeClient.RunResult("Accepted", 0, 1, 128, "41\n", "", ""));
        Problem problem = insertOjProblem();
        Submission submission = insertOjSubmission(102L, problem.getId());

        publisher.publish(new TaskMessage(submission.getId(), TaskType.JUDGE_OJ));

        Submission finished = awaitSubmission(submission.getId(),
                Duration.ofSeconds(20), "finished");
        assertEquals("WA", finished.getVerdict());
        WrongBook wrongBook = wrongBookMapper.selectOne(Wrappers.<WrongBook>lambdaQuery()
                .eq(WrongBook::getUserId, 102L)
                .eq(WrongBook::getProblemId, problem.getId()));
        assertNotNull(wrongBook, "非 AC 结果应写入错题本");
        assertEquals("WA", wrongBook.getLatestError());
        assertEquals(1, wrongBook.getErrorCount().intValue());
    }

    @Test
    void shortAnswerIsGradedAndWrittenBack() {
        when(llmClient.grade(anyString(), anyString(), anyString(), anyList(), anyString()))
                .thenReturn(new PlatformLlmClient.GradingResult(
                        new BigDecimal("55"), "缺少关键点"));
        Problem problem = insertShortAnswerProblem();
        Submission submission = insertShortAnswerSubmission(103L, problem.getId());

        publisher.publish(new TaskMessage(submission.getId(), TaskType.JUDGE_SHORT_ANSWER));

        Submission finished = awaitSubmission(submission.getId(),
                Duration.ofSeconds(20), "finished");
        assertEquals("WA", finished.getVerdict());
        assertEquals(0, new BigDecimal("55").compareTo(finished.getScore()));
        assertEquals("缺少关键点", finished.getAiFeedback());
        WrongBook wrongBook = wrongBookMapper.selectOne(Wrappers.<WrongBook>lambdaQuery()
                .eq(WrongBook::getUserId, 103L)
                .eq(WrongBook::getProblemId, problem.getId()));
        assertNotNull(wrongBook, "简答不及格应写入错题本");
        assertEquals("AI 55", wrongBook.getLatestError());
    }

    @Test
    void datagenRandomTaskStoresFiles() {
        DataGenTask task = insertDatagenTask(104L, 2);

        publisher.publish(new TaskMessage(task.getId(), TaskType.DATAGEN));

        DataGenTask finished = awaitDatagenTask(task.getId(),
                Duration.ofSeconds(20), "success");
        assertEquals(1, finished.getAttempts().intValue());
        List<DataGenFile> files = dataGenFileMapper.selectList(
                Wrappers.<DataGenFile>lambdaQuery().eq(DataGenFile::getTaskId, task.getId()));
        assertEquals(2, files.size());
        assertTrue(files.stream().allMatch(file -> "passed".equals(file.getValidationStatus())));
        verify(goJudgeClient, times(1)).compile(anyString(), anyString());
        verify(goJudgeClient, times(2)).runTest(any(), anyString(), anyInt(), anyInt());
    }

    @Test
    void duplicateDeliveryExecutesJudgeOnce() {
        Problem problem = insertOjProblem();
        Submission submission = insertOjSubmission(105L, problem.getId());
        TaskMessage message = new TaskMessage(submission.getId(), TaskType.JUDGE_OJ);

        publisher.publish(message);
        publisher.publish(message);

        Submission finished = awaitSubmission(submission.getId(),
                Duration.ofSeconds(20), "finished");
        assertEquals("AC", finished.getVerdict());
        assertEquals(1, finished.getJudgeAttempts().intValue(), "重复消息不应重复占用任务");
        sleep(1_000);
        verify(goJudgeClient, times(1)).compile(anyString(), anyString());
    }

    @Test
    void exhaustedRetriesMarkFailedAndDeadLetter() {
        doThrow(new GoJudgeClient.GoJudgeException("沙箱不可用"))
                .when(goJudgeClient).compile(anyString(), anyString());
        Problem problem = insertOjProblem();
        Submission submission = insertOjSubmission(106L, problem.getId());

        publisher.publish(new TaskMessage(submission.getId(), TaskType.JUDGE_OJ));

        Submission failed = awaitSubmission(submission.getId(),
                Duration.ofSeconds(25), "failed");
        assertEquals("判题服务暂不可用，请稍后重新提交", failed.getFailureReason());
        assertEquals(MAX_ATTEMPTS, failed.getJudgeAttempts().intValue());
        verify(goJudgeClient, times(MAX_ATTEMPTS)).compile(anyString(), anyString());
        Message deadLetter = receive(TaskType.JUDGE_OJ.deadLetterQueue(), 10_000);
        assertNotNull(deadLetter, "重试耗尽后应进入死信队列");
        assertEquals(new TaskMessage(submission.getId(), TaskType.JUDGE_OJ),
                messageConverter.fromMessage(deadLetter));
    }

    @Test
    void staleInProgressTaskIsRecoveredAndReprocessed() {
        Problem problem = insertOjProblem();
        Submission submission = insertOjSubmission(107L, problem.getId());
        Submission claimed = taskService.claimById(submission.getId(), false, MAX_ATTEMPTS);
        assertNotNull(claimed);
        assertEquals("judging", claimed.getStatus());

        // 模拟 worker 在执行中被终止：任务停留在 judging，超时扫描把它放回 pending。
        taskService.recoverStale(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(5), MAX_ATTEMPTS);
        Submission recovered = submissionMapper.selectById(submission.getId());
        assertEquals("pending", recovered.getStatus());
        assertNull(recovered.getJudgeStartedAt());

        publisher.publish(new TaskMessage(submission.getId(), TaskType.JUDGE_OJ));

        Submission finished = awaitSubmission(submission.getId(),
                Duration.ofSeconds(20), "finished");
        assertEquals("AC", finished.getVerdict());
        assertEquals(2, finished.getJudgeAttempts().intValue());
    }

    @Test
    void undispatchedTaskIsCompensatedByScanner() {
        Problem problem = insertOjProblem();
        Submission submission = insertOjSubmission(108L, problem.getId());
        submission.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(10));
        submissionMapper.updateById(submission);

        compensationScanner.scan();

        Submission finished = awaitSubmission(submission.getId(),
                Duration.ofSeconds(20), "finished");
        assertEquals("AC", finished.getVerdict());
    }

    @Test
    void hundredOjMessagesCompleteWithinTimeBudget() {
        Problem problem = insertOjProblem();
        List<Long> submissionIds = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            Submission submission = insertOjSubmission(1_000L + index, problem.getId());
            submissionIds.add(submission.getId());
        }

        long startedAt = System.nanoTime();
        for (Long submissionId : submissionIds) {
            publisher.publish(new TaskMessage(submissionId, TaskType.JUDGE_OJ));
        }
        for (Long submissionId : submissionIds) {
            Submission finished = awaitSubmission(submissionId, Duration.ofSeconds(90),
                    "finished");
            assertEquals("AC", finished.getVerdict());
        }
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;

        assertTrue(elapsedMs < 60_000, "100 条判题消息应在 60 秒内处理完，实际 "
                + elapsedMs + " ms");
    }

    private Problem insertOjProblem() {
        Problem problem = new Problem();
        problem.setType("oj");
        problem.setTitle("A + B");
        problem.setStatement("输出两个整数之和");
        problem.setInputFormat("一行两个整数");
        problem.setOutputFormat("一个整数");
        problem.setDifficulty("entry");
        problem.setSource("测试");
        problem.setTimeLimitMs(1_000);
        problem.setMemoryLimitMb(256);
        problem.setStatus("published");
        problemMapper.insert(problem);

        TestData testData = new TestData();
        testData.setProblemId(problem.getId());
        testData.setInputData("1 2\n");
        testData.setOutputData(EXPECTED_OUTPUT);
        testData.setSortOrder(1);
        testDataMapper.insert(testData);
        return problem;
    }

    private Problem insertShortAnswerProblem() {
        Problem problem = new Problem();
        problem.setType("short_answer");
        problem.setTitle("解释闭包");
        problem.setStatement("简述什么是闭包");
        problem.setReferenceAnswer("函数与其词法作用域的组合");
        problem.setScoringRubric("[{\"point\":\"能说明作用域\",\"score\":60}]");
        problem.setMaxScore(100);
        problem.setDifficulty("basic");
        problem.setSource("测试");
        problem.setStatus("published");
        problemMapper.insert(problem);
        return problem;
    }

    private Submission insertOjSubmission(long userId, Long problemId) {
        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(problemId);
        submission.setProblemType("oj");
        submission.setLanguage("cpp17");
        submission.setCode("int main() { return 0; }");
        submission.setStatus("pending");
        submission.setJudgeAttempts(0);
        submission.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        submissionMapper.insert(submission);
        return submission;
    }

    private Submission insertShortAnswerSubmission(long userId, Long problemId) {
        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(problemId);
        submission.setProblemType("short_answer");
        submission.setAnswerText("闭包是函数和它捕获的变量环境");
        submission.setStatus("grading");
        submission.setJudgeAttempts(0);
        submission.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        submissionMapper.insert(submission);
        return submission;
    }

    private DataGenTask insertDatagenTask(long userId, int groupCount) {
        DataGenTask task = new DataGenTask();
        task.setUserId(userId);
        task.setTitle("A + B");
        task.setStatement("输出两个整数之和");
        task.setInputFormat("一行两个整数");
        task.setOutputFormat("一个整数");
        task.setStdCode("int main() { return 0; }");
        task.setStdLanguage("cpp17");
        task.setDataRange("[{\"name\":\"a\",\"type\":\"int\",\"min\":1,\"max\":10},"
                + "{\"name\":\"b\",\"type\":\"int\",\"min\":1,\"max\":10}]");
        task.setGroupCount(groupCount);
        task.setGenerationMode("random");
        task.setStatus("pending");
        task.setAttempts(0);
        task.setTimeLimitMs(1_000);
        task.setMemoryLimitMb(256);
        task.setFilesExpireAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7));
        task.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        dataGenTaskMapper.insert(task);
        return task;
    }

    private Submission awaitSubmission(Long submissionId, Duration timeout, String status) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        Submission latest = null;
        while (System.currentTimeMillis() < deadline) {
            latest = submissionMapper.selectById(submissionId);
            if (latest != null && status.equals(latest.getStatus())) {
                return latest;
            }
            sleep(100);
        }
        throw new AssertionError("提交未在超时内进入 " + status + "，当前状态："
                + (latest == null ? "null" : latest.getStatus()));
    }

    private DataGenTask awaitDatagenTask(Long taskId, Duration timeout, String status) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        DataGenTask latest = null;
        while (System.currentTimeMillis() < deadline) {
            latest = dataGenTaskMapper.selectById(taskId);
            if (latest != null && status.equals(latest.getStatus())) {
                return latest;
            }
            sleep(100);
        }
        throw new AssertionError("造数据任务未在超时内进入 " + status + "，当前状态："
                + (latest == null ? "null" : latest.getStatus()));
    }

    private Message receive(String queue, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Message message = rabbitTemplate.receive(queue, 300);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan(basePackages = {
            "com.tizhen.core.judge.mapper",
            "com.tizhen.core.problem.mapper",
            "com.tizhen.core.datagen.mapper",
            "com.tizhen.core.battle.mapper"
    }, annotationClass = Mapper.class)
    @Import({MqConfig.class, MqListenerConfig.class, TaskPublisher.class,
            TaskDispatchListener.class, TaskConsumerSupport.class,
            TaskCompensationScanner.class, JudgeMqConsumer.class, DataGenMqConsumer.class,
            JudgeWorker.class, DataGenWorker.class,
            SubmissionTaskService.class, JudgeResultService.class,
            BattleLifecycleService.class, DataGenTaskStateService.class})
    static class TestApplication {

        @Bean
        GoJudgeClient goJudgeClient() {
            return mock(GoJudgeClient.class);
        }

        @Bean
        PlatformLlmClient platformLlmClient() {
            return mock(PlatformLlmClient.class);
        }

        @Bean
        LlmCredentialService llmCredentialService() {
            return mock(LlmCredentialService.class);
        }

        @Bean
        UserLlmClient userLlmClient() {
            return mock(UserLlmClient.class);
        }

        @Bean
        LlmProviderRegistry llmProviderRegistry() {
            return mock(LlmProviderRegistry.class);
        }

        @Bean
        FileStorage fileStorage() {
            return mock(FileStorage.class);
        }
    }
}
