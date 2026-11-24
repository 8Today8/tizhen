package com.tizhen.worker.mq;

import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.model.JudgeOutcome;
import com.tizhen.core.judge.service.JudgeResultService;
import com.tizhen.core.judge.service.SubmissionTaskService;
import com.tizhen.core.mq.MqConfig;
import com.tizhen.core.mq.MqNames;
import com.tizhen.core.mq.TaskMessage;
import com.tizhen.core.mq.TaskPublisher;
import com.tizhen.core.mq.TaskRetryableException;
import com.tizhen.core.mq.TaskType;
import com.tizhen.testsupport.RabbitMqTestBase;
import com.tizhen.worker.judge.JudgeMqConsumer;
import com.tizhen.worker.judge.JudgeWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 真实 broker 的消费链路集成测试：验证序列化、监听容器、手动 ack、
 * 延迟重试、次数用尽进死信，以及毒消息不阻塞后续消息。
 * 执行业务逻辑的 worker 用 Mock 替换，只验证消息链路本身。
 */
@SpringBootTest(classes = TaskMessagingIntegrationTest.TestApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                "spring.rabbitmq.dynamic=false",
                "tizhen.mq.enabled=true",
                "tizhen.mq.retry-delays=1s",
                "tizhen.mq.scan-interval-ms=600000",
                "tizhen.judge.max-attempts=3",
                "worker.judge-concurrency=1",
                "worker.datagen-concurrency=1"
        })
class TaskMessagingIntegrationTest extends RabbitMqTestBase {

    private static final long FIRST_ID = 1L;
    private static final long SECOND_ID = 2L;
    private static final int MAX_ATTEMPTS = 3;

    @Autowired
    private TaskPublisher publisher;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private MessageConverter messageConverter;
    @Autowired
    private SubmissionTaskService taskService;
    @Autowired
    private JudgeResultService resultService;
    @Autowired
    private JudgeWorker judgeWorker;

    @BeforeEach
    void resetState() {
        Mockito.reset(taskService, resultService, judgeWorker);
        for (TaskType taskType : TaskType.values()) {
            amqpAdmin.purgeQueue(taskType.queue(), false);
            amqpAdmin.purgeQueue(taskType.deadLetterQueue(), false);
            amqpAdmin.purgeQueue(MqNames.retryQueue(taskType, Duration.ofSeconds(1)), false);
        }
    }

    @Test
    void publishedMessageIsConsumedAndAcked() {
        Submission submission = submission(FIRST_ID, 1);
        when(taskService.claimById(FIRST_ID, false, MAX_ATTEMPTS)).thenReturn(submission);

        publisher.publish(new TaskMessage(FIRST_ID, TaskType.JUDGE_OJ));

        verify(judgeWorker, timeout(10_000)).judgeOj(any());
        assertQueueEmpty(TaskType.JUDGE_OJ.queue());
    }

    @Test
    void retryableFailureGoesThroughDelayQueueThenDeadLetter() {
        when(taskService.claimById(FIRST_ID, false, MAX_ATTEMPTS))
                .thenReturn(submission(FIRST_ID, 1));
        when(taskService.claimById(FIRST_ID, true, MAX_ATTEMPTS))
                .thenReturn(submission(FIRST_ID, MAX_ATTEMPTS));
        when(resultService.complete(FIRST_ID, JudgeOutcome.failed("沙箱不可用")))
                .thenReturn(true);
        doThrow(new TaskRetryableException("沙箱不可用")).when(judgeWorker).judgeOj(any());

        publisher.publish(new TaskMessage(FIRST_ID, TaskType.JUDGE_OJ));

        verify(judgeWorker, timeout(15_000).times(2)).judgeOj(any());
        verify(resultService, timeout(5_000))
                .complete(FIRST_ID, JudgeOutcome.failed("沙箱不可用"));
        Message deadLetter = receive(TaskType.JUDGE_OJ.deadLetterQueue(), 5_000);
        assertNotNull(deadLetter, "达到尝试上限后应进入死信队列");
        assertEquals(new TaskMessage(FIRST_ID, TaskType.JUDGE_OJ),
                messageConverter.fromMessage(deadLetter));
    }

    @Test
    void duplicateMessageExecutesOnlyOnce() {
        when(taskService.claimById(SECOND_ID, false, MAX_ATTEMPTS))
                .thenReturn(submission(SECOND_ID, 1))
                .thenReturn(null);
        TaskMessage message = new TaskMessage(SECOND_ID, TaskType.JUDGE_OJ);

        publisher.publish(message);
        publisher.publish(message);

        verify(judgeWorker, timeout(10_000).times(1)).judgeOj(any());
        verify(taskService, timeout(5_000).times(2))
                .claimById(SECOND_ID, false, MAX_ATTEMPTS);
        sleep(500);
        verify(judgeWorker, Mockito.times(1)).judgeOj(any());
    }

    @Test
    void poisonMessageIsDeadLetteredWithoutBlockingQueue() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message poison = new Message("{not-json".getBytes(StandardCharsets.UTF_8), properties);

        rabbitTemplate.send(MqNames.MAIN_EXCHANGE, TaskType.JUDGE_OJ.routingKey(), poison);

        Message deadLetter = receive(TaskType.JUDGE_OJ.deadLetterQueue(), 10_000);
        assertNotNull(deadLetter, "反序列化失败的消息应进入死信队列");
        assertEquals("{not-json",
                new String(deadLetter.getBody(), StandardCharsets.UTF_8));
        verify(judgeWorker, Mockito.never()).judgeOj(any());
    }

    private Submission submission(long id, int attempts) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setProblemType("oj");
        submission.setJudgeAttempts(attempts);
        return submission;
    }

    private void assertQueueEmpty(String queue) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            Message remaining = rabbitTemplate.receive(queue, 100);
            if (remaining == null) {
                return;
            }
        }
        throw new AssertionError("队列仍有未消费消息: " + queue);
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
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({MqConfig.class, TaskPublisher.class, MqListenerConfig.class,
            TaskConsumerSupport.class, JudgeMqConsumer.class})
    static class TestApplication {

        @Bean
        SubmissionTaskService submissionTaskService() {
            return mock(SubmissionTaskService.class);
        }

        @Bean
        JudgeResultService judgeResultService() {
            return mock(JudgeResultService.class);
        }

        @Bean
        JudgeWorker judgeWorker() {
            return mock(JudgeWorker.class);
        }
    }
}
