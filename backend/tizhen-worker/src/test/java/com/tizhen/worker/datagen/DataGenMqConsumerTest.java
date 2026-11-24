package com.tizhen.worker.datagen;

import com.rabbitmq.client.Channel;
import com.tizhen.core.datagen.entity.DataGenTask;
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
 * 造数据消费者的状态机测试：复用执行逻辑，临时故障重试，达到上限落失败。
 */
@ExtendWith(MockitoExtension.class)
class DataGenMqConsumerTest {

    private static final long TASK_ID = 9L;
    private static final long DELIVERY_TAG = 20L;
    private static final int MAX_ATTEMPTS = 2;

    @Mock
    private DataGenTaskStateService stateService;
    @Mock
    private DataGenWorker dataGenWorker;
    @Mock
    private TaskPublisher publisher;
    @Mock
    private TaskConsumerSupport support;
    @Mock
    private Channel channel;

    private DataGenMqConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new DataGenMqConsumer(stateService, dataGenWorker, publisher, support,
                MAX_ATTEMPTS);
    }

    @Test
    void duplicateMessageIsAckedWithoutExecution() throws IOException {
        TaskMessage message = new TaskMessage(TASK_ID, TaskType.DATAGEN);
        when(stateService.claimById(TASK_ID, false, MAX_ATTEMPTS)).thenReturn(null);

        consumer.onDataGenTask(message, channel, DELIVERY_TAG, null, false);

        verify(support).ack(channel, DELIVERY_TAG);
        verify(dataGenWorker, never()).process(any());
        verify(stateService, never()).fail(anyLong(), any(), any());
    }

    @Test
    void retryableFailurePublishesRetry() throws IOException {
        TaskMessage message = new TaskMessage(TASK_ID, TaskType.DATAGEN);
        DataGenTask task = task(1);
        when(stateService.claimById(TASK_ID, false, MAX_ATTEMPTS)).thenReturn(task);
        doThrow(new TaskRetryableException("system", "沙箱不可用"))
                .when(dataGenWorker).process(task);

        consumer.onDataGenTask(message, channel, DELIVERY_TAG, null, false);

        verify(support).retry(message, 1, channel, DELIVERY_TAG);
        verify(stateService, never()).fail(anyLong(), any(), any());
    }

    @Test
    void exhaustedAttemptsFailAndGoToDeadLetter() throws IOException {
        TaskMessage message = new TaskMessage(TASK_ID, TaskType.DATAGEN);
        DataGenTask task = task(MAX_ATTEMPTS);
        when(stateService.claimById(TASK_ID, true, MAX_ATTEMPTS)).thenReturn(task);
        doThrow(new TaskRetryableException("system", "沙箱不可用"))
                .when(dataGenWorker).process(task);

        consumer.onDataGenTask(message, channel, DELIVERY_TAG, 1, false);

        verify(stateService).fail(TASK_ID, "system", "沙箱不可用");
        verify(publisher).publishDeadLetter(message);
        verify(support).ack(channel, DELIVERY_TAG);
    }

    @Test
    void claimFailureNacksForRedelivery() throws IOException {
        TaskMessage message = new TaskMessage(TASK_ID, TaskType.DATAGEN);
        when(stateService.claimById(TASK_ID, false, MAX_ATTEMPTS))
                .thenThrow(new RuntimeException("db down"));

        consumer.onDataGenTask(message, channel, DELIVERY_TAG, null, false);

        verify(support).nack(channel, DELIVERY_TAG);
        verify(support, never()).ack(channel, DELIVERY_TAG);
    }

    private DataGenTask task(int attempts) {
        DataGenTask task = new DataGenTask();
        task.setId(TASK_ID);
        task.setGenerationMode("random");
        task.setAttempts(attempts);
        return task;
    }
}
