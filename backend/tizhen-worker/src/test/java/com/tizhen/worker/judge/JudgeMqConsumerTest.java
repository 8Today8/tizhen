package com.tizhen.worker.judge;

import com.rabbitmq.client.Channel;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.model.JudgeOutcome;
import com.tizhen.core.judge.service.JudgeResultService;
import com.tizhen.core.judge.service.SubmissionTaskService;
import com.tizhen.core.mq.TaskMessage;
import com.tizhen.core.mq.TaskPublisher;
import com.tizhen.core.mq.TaskRetryableException;
import com.tizhen.core.mq.TaskType;
import com.tizhen.worker.mq.TaskConsumerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 判题消费者的状态机测试：重复消息、成功、重试、终态与领取失败。
 */
@ExtendWith(MockitoExtension.class)
class JudgeMqConsumerTest {

    private static final long SUBMISSION_ID = 1L;
    private static final long DELIVERY_TAG = 10L;
    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private SubmissionTaskService taskService;
    @Mock
    private JudgeResultService resultService;
    @Mock
    private JudgeWorker judgeWorker;
    @Mock
    private TaskPublisher publisher;
    @Mock
    private TaskConsumerSupport support;
    @Mock
    private Channel channel;

    private JudgeMqConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new JudgeMqConsumer(taskService, resultService, judgeWorker,
                publisher, support, MAX_ATTEMPTS);
    }

    @Test
    void duplicateMessageIsAckedWithoutExecution() throws IOException {
        TaskMessage message = new TaskMessage(SUBMISSION_ID, TaskType.JUDGE_OJ);
        when(taskService.claimById(SUBMISSION_ID, false, MAX_ATTEMPTS)).thenReturn(null);

        consumer.onOjTask(message, channel, DELIVERY_TAG, null, false);

        verify(support).ack(channel, DELIVERY_TAG);
        verify(judgeWorker, never()).judgeOj(any());
        verify(resultService, never()).complete(anyLong(), any());
        verify(publisher, never()).publishRetry(any(), anyInt());
    }

    @Test
    void successfulExecutionIsAcked() throws IOException {
        TaskMessage message = new TaskMessage(SUBMISSION_ID, TaskType.JUDGE_OJ);
        Submission task = submission(1);
        when(taskService.claimById(SUBMISSION_ID, false, MAX_ATTEMPTS)).thenReturn(task);

        consumer.onOjTask(message, channel, DELIVERY_TAG, null, false);

        verify(judgeWorker).judgeOj(task);
        verify(support).ack(channel, DELIVERY_TAG);
        verify(support, never()).retry(any(), org.mockito.ArgumentMatchers.anyInt(),
                any(), anyLong());
    }

    @Test
    void retryableFailurePublishesNextRetryLevel() throws IOException {
        TaskMessage message = new TaskMessage(SUBMISSION_ID, TaskType.JUDGE_OJ);
        Submission task = submission(1);
        when(taskService.claimById(SUBMISSION_ID, false, MAX_ATTEMPTS)).thenReturn(task);
        doThrow(new TaskRetryableException("沙箱不可用")).when(judgeWorker).judgeOj(task);

        consumer.onOjTask(message, channel, DELIVERY_TAG, null, false);

        verify(support).retry(message, 1, channel, DELIVERY_TAG);
        verify(resultService, never()).complete(anyLong(), any());
    }

    @Test
    void exhaustedAttemptsFailAndGoToDeadLetter() throws IOException {
        TaskMessage message = new TaskMessage(SUBMISSION_ID, TaskType.JUDGE_OJ);
        Submission task = submission(MAX_ATTEMPTS);
        when(taskService.claimById(SUBMISSION_ID, true, MAX_ATTEMPTS)).thenReturn(task);
        doThrow(new TaskRetryableException("判题失败")).when(judgeWorker).judgeOj(task);
        when(resultService.complete(SUBMISSION_ID, JudgeOutcome.failed("判题失败")))
                .thenReturn(true);

        consumer.onOjTask(message, channel, DELIVERY_TAG, 1, false);

        verify(resultService).complete(SUBMISSION_ID, JudgeOutcome.failed("判题失败"));
        verify(publisher).publishDeadLetter(message);
        verify(support).ack(channel, DELIVERY_TAG);
    }

    @Test
    void redeliveredMessageTakesOverInProgressTask() throws IOException {
        TaskMessage message = new TaskMessage(SUBMISSION_ID, TaskType.JUDGE_OJ);
        Submission task = submission(2);
        when(taskService.claimById(SUBMISSION_ID, true, MAX_ATTEMPTS)).thenReturn(task);

        consumer.onOjTask(message, channel, DELIVERY_TAG, null, true);

        verify(taskService).claimById(SUBMISSION_ID, true, MAX_ATTEMPTS);
        verify(judgeWorker).judgeOj(task);
        verify(support).ack(channel, DELIVERY_TAG);
    }

    @Test
    void claimFailureNacksForRedelivery() throws IOException {
        TaskMessage message = new TaskMessage(SUBMISSION_ID, TaskType.JUDGE_OJ);
        when(taskService.claimById(SUBMISSION_ID, false, MAX_ATTEMPTS))
                .thenThrow(new RuntimeException("db down"));

        consumer.onOjTask(message, channel, DELIVERY_TAG, null, false);

        verify(support).nack(channel, DELIVERY_TAG);
        verify(support, never()).ack(channel, DELIVERY_TAG);
    }

    private Submission submission(int attempts) {
        Submission submission = new Submission();
        submission.setId(SUBMISSION_ID);
        submission.setProblemType("oj");
        submission.setJudgeAttempts(attempts);
        return submission;
    }
}
