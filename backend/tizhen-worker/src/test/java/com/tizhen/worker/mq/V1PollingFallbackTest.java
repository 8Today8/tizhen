package com.tizhen.worker.mq;

import com.tizhen.core.account.service.LlmCredentialService;
import com.tizhen.core.battle.mapper.BattleRoomMapper;
import com.tizhen.core.battle.service.BattleLifecycleService;
import com.tizhen.core.datagen.entity.DataGenFile;
import com.tizhen.core.datagen.entity.DataGenTask;
import com.tizhen.core.datagen.mapper.DataGenFileMapper;
import com.tizhen.core.datagen.mapper.DataGenTaskMapper;
import com.tizhen.core.infra.file.FileStorage;
import com.tizhen.core.infra.file.StoredFile;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.mapper.FirstAcMapper;
import com.tizhen.core.judge.mapper.SubmissionMapper;
import com.tizhen.core.judge.mapper.WrongBookMapper;
import com.tizhen.core.judge.service.JudgeResultService;
import com.tizhen.core.judge.service.SubmissionTaskService;
import com.tizhen.core.llm.LlmProviderRegistry;
import com.tizhen.core.llm.UserLlmClient;
import com.tizhen.core.mq.TaskPublisher;
import com.tizhen.core.problem.entity.Problem;
import com.tizhen.core.problem.entity.TestData;
import com.tizhen.core.problem.mapper.ProblemMapper;
import com.tizhen.core.problem.mapper.TestDataMapper;
import com.tizhen.testsupport.IntegrationTestBase;
import com.tizhen.worker.datagen.DataGenTaskStateService;
import com.tizhen.worker.datagen.DataGenWorker;
import com.tizhen.worker.judge.GoJudgeClient;
import com.tizhen.worker.judge.JudgeWorker;
import com.tizhen.worker.judge.PlatformLlmClient;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V2 开关关闭时的 V1 回归测试：不连接 broker、不加载消息组件，
 * 由数据库轮询驱动真实执行器完成判题与造数据，并验证临时故障按 V1 口径落失败。
 */
@SpringBootTest(classes = V1PollingFallbackTest.TestApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                "tizhen.mq.enabled=false",
                "tizhen.judge.max-attempts=3",
                "worker.judge-concurrency=4",
                "worker.datagen-concurrency=2",
                "worker.datagen-max-attempts=2"
        })
class V1PollingFallbackTest extends IntegrationTestBase {

    private static final String EXPECTED_OUTPUT = "42\n";

    @Autowired
    private ApplicationContext context;
    @Autowired
    private JudgeWorker judgeWorker;
    @Autowired
    private DataGenWorker dataGenWorker;
    @Autowired
    private GoJudgeClient goJudgeClient;
    @Autowired
    private FileStorage fileStorage;
    @Autowired
    private SubmissionMapper submissionMapper;
    @Autowired
    private ProblemMapper problemMapper;
    @Autowired
    private TestDataMapper testDataMapper;
    @Autowired
    private DataGenTaskMapper dataGenTaskMapper;
    @Autowired
    private DataGenFileMapper dataGenFileMapper;

    @BeforeEach
    void resetState() {
        Mockito.reset(goJudgeClient, fileStorage);
        when(goJudgeClient.compile(anyString(), anyString())).thenReturn(
                new GoJudgeClient.CompileResult(true, "",
                        new GoJudgeClient.CompiledProgram("cpp17", "source", null)));
        when(goJudgeClient.runTest(any(), anyString(), anyInt(), anyInt())).thenReturn(
                new GoJudgeClient.RunResult("Accepted", 0, 1, 128,
                        EXPECTED_OUTPUT, "", ""));
        try {
            doAnswer(invocation -> new StoredFile("polling/file-" + System.nanoTime(), 0))
                    .when(fileStorage).store(any(InputStream.class), anyString());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void switchOffLoadsNoMessagingBeansAndPollsJudging() {
        assertTrue(context.getBeansOfType(TaskPublisher.class).isEmpty(),
                "开关关闭时不应加载消息发布器");
        assertTrue(context.getBeansOfType(TaskCompensationScanner.class).isEmpty(),
                "开关关闭时不应加载补偿扫描");
        assertTrue(context.getBeansOfType(RabbitTemplate.class).isEmpty(),
                "开关关闭时不应创建 RabbitTemplate");

        Problem problem = insertOjProblem();
        Submission submission = insertOjSubmission(201L, problem.getId());

        judgeWorker.poll();

        Submission finished = awaitSubmission(submission.getId(),
                Duration.ofSeconds(20), "finished");
        assertEquals("AC", finished.getVerdict());
        assertEquals(1, finished.getJudgeAttempts().intValue());
    }

    @Test
    void pollingMarksRetryableFailureAsFailed() {
        doThrow(new GoJudgeClient.GoJudgeException("沙箱不可用"))
                .when(goJudgeClient).compile(anyString(), anyString());
        Problem problem = insertOjProblem();
        Submission submission = insertOjSubmission(202L, problem.getId());

        judgeWorker.poll();

        Submission failed = awaitSubmission(submission.getId(),
                Duration.ofSeconds(20), "failed");
        assertEquals("判题服务暂不可用，请稍后重新提交", failed.getFailureReason());
        assertEquals(1, failed.getJudgeAttempts().intValue());
    }

    @Test
    void pollingRunsDatagenTask() {
        DataGenTask task = insertDatagenTask(203L, 2);

        dataGenWorker.poll();

        DataGenTask finished = awaitDatagenTask(task.getId(),
                Duration.ofSeconds(20), "success");
        assertEquals(1, finished.getAttempts().intValue());
        List<DataGenFile> files = dataGenFileMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<DataGenFile>lambdaQuery()
                        .eq(DataGenFile::getTaskId, task.getId()));
        assertEquals(2, files.size());
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

    private DataGenTask insertDatagenTask(long userId, int groupCount) {
        DataGenTask task = new DataGenTask();
        task.setUserId(userId);
        task.setTitle("A + B");
        task.setStatement("输出两个整数之和");
        task.setInputFormat("一行两个整数");
        task.setOutputFormat("一个整数");
        task.setStdCode("int main() { return 0; }");
        task.setStdLanguage("cpp17");
        task.setDataRange("[{\"name\":\"a\",\"type\":\"int\",\"min\":1,\"max\":10}]");
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

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
    @MapperScan(basePackages = {
            "com.tizhen.core.judge.mapper",
            "com.tizhen.core.problem.mapper",
            "com.tizhen.core.datagen.mapper",
            "com.tizhen.core.battle.mapper"
    }, annotationClass = Mapper.class)
    @Import({JudgeWorker.class, DataGenWorker.class,
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
