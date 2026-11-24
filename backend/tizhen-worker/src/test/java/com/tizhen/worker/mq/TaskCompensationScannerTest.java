package com.tizhen.worker.mq;

import com.tizhen.core.datagen.entity.DataGenTask;
import com.tizhen.core.datagen.mapper.DataGenTaskMapper;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.mapper.SubmissionMapper;
import com.tizhen.core.mq.MqProperties;
import com.tizhen.core.mq.TaskMessage;
import com.tizhen.core.mq.TaskPublisher;
import com.tizhen.core.mq.TaskType;
import com.tizhen.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 补偿扫描测试：使用真实 Redis 验证去重键的抢占、过期与失败释放，
 * 数据库查询与消息投递用 Mock 替换，从而精确断言扫描范围与投递次数。
 */
@SpringBootTest(classes = TaskCompensationScannerTest.TestApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                "tizhen.mq.enabled=true",
                "tizhen.mq.stale-seconds=5",
                "tizhen.mq.scan-interval-ms=600000"
        })
class TaskCompensationScannerTest extends IntegrationTestBase {

    private static final String KEY_PREFIX = "tizhen:mq:compensate:";

    @Autowired
    private TaskCompensationScanner scanner;
    @Autowired
    private TaskPublisher publisher;
    @Autowired
    private SubmissionMapper submissionMapper;
    @Autowired
    private DataGenTaskMapper dataGenTaskMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void resetState() {
        Mockito.reset(publisher, submissionMapper, dataGenTaskMapper);
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        when(dataGenTaskMapper.selectPendingBefore(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of());
    }

    @Test
    void scanPublishesUndispatchedTasksOncePerWindow() {
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);
        when(submissionMapper.selectUndispatched(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(ojSubmission(301L), shortAnswerSubmission(302L)));
        when(dataGenTaskMapper.selectPendingBefore(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(dataGenTask(303L)));

        scanner.scan();
        scanner.scan();

        ArgumentCaptor<TaskMessage> messages = ArgumentCaptor.forClass(TaskMessage.class);
        verify(publisher, times(3)).publish(messages.capture());
        assertTrue(messages.getAllValues().contains(new TaskMessage(301L, TaskType.JUDGE_OJ)));
        assertTrue(messages.getAllValues().contains(
                new TaskMessage(302L, TaskType.JUDGE_SHORT_ANSWER)));
        assertTrue(messages.getAllValues().contains(new TaskMessage(303L, TaskType.DATAGEN)));

        ArgumentCaptor<LocalDateTime> threshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(submissionMapper, times(2)).selectUndispatched(threshold.capture(), eq(100));
        LocalDateTime expected = before.minusSeconds(5);
        LocalDateTime firstThreshold = threshold.getAllValues().get(0);
        assertTrue(firstThreshold.isAfter(expected.minusSeconds(1))
                        && firstThreshold.isBefore(expected.plusSeconds(5)),
                "扫描阈值应约为当前时间减 stale-seconds，实际 " + firstThreshold);
    }

    @Test
    void publishFailureReleasesDedupeKeyAndDoesNotAbortScan() {
        when(submissionMapper.selectUndispatched(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(ojSubmission(311L), ojSubmission(312L)));
        doThrow(new IllegalStateException("broker 不可用"))
                .doNothing()
                .when(publisher).publish(new TaskMessage(311L, TaskType.JUDGE_OJ));

        scanner.scan();

        verify(publisher).publish(new TaskMessage(311L, TaskType.JUDGE_OJ));
        verify(publisher).publish(new TaskMessage(312L, TaskType.JUDGE_OJ));

        // 失败任务的去重键已释放，第二次扫描可再次投递；成功的任务仍被窗口去重。
        scanner.scan();
        verify(publisher, times(2)).publish(new TaskMessage(311L, TaskType.JUDGE_OJ));
        verify(publisher, times(1)).publish(new TaskMessage(312L, TaskType.JUDGE_OJ));
    }

    private Submission ojSubmission(Long id) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setProblemType("oj");
        return submission;
    }

    private Submission shortAnswerSubmission(Long id) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setProblemType("short_answer");
        return submission;
    }

    private DataGenTask dataGenTask(Long id) {
        DataGenTask task = new DataGenTask();
        task.setId(id);
        return task;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
    @EnableConfigurationProperties(MqProperties.class)
    @Import(TaskCompensationScanner.class)
    static class TestApplication {

        @Bean
        TaskPublisher taskPublisher() {
            return mock(TaskPublisher.class);
        }

        @Bean
        SubmissionMapper submissionMapper() {
            return mock(SubmissionMapper.class);
        }

        @Bean
        DataGenTaskMapper dataGenTaskMapper() {
            return mock(DataGenTaskMapper.class);
        }
    }
}
