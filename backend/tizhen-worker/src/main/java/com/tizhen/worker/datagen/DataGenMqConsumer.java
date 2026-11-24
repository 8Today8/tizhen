package com.tizhen.worker.datagen;

import com.rabbitmq.client.Channel;
import com.tizhen.core.datagen.entity.DataGenTask;
import com.tizhen.core.mq.MqNames;
import com.tizhen.core.mq.TaskMessage;
import com.tizhen.core.mq.TaskPublisher;
import com.tizhen.core.mq.TaskRetryableException;
import com.tizhen.core.mq.TaskType;
import com.tizhen.worker.mq.TaskConsumerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 造数据消息消费者。执行逻辑复用 DataGenWorker，临时故障进入延迟重试，
 * 达到上限后落失败并投递死信。
 */
@Component
@ConditionalOnProperty(prefix = "tizhen.mq", name = "enabled", havingValue = "true")
public class DataGenMqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DataGenMqConsumer.class);

    private final DataGenTaskStateService stateService;
    private final DataGenWorker dataGenWorker;
    private final TaskPublisher publisher;
    private final TaskConsumerSupport support;
    private final int maxAttempts;

    public DataGenMqConsumer(DataGenTaskStateService stateService,
                             DataGenWorker dataGenWorker,
                             TaskPublisher publisher,
                             TaskConsumerSupport support,
                             @Value("${worker.datagen-max-attempts:2}") int maxAttempts) {
        this.stateService = stateService;
        this.dataGenWorker = dataGenWorker;
        this.publisher = publisher;
        this.support = support;
        this.maxAttempts = Math.max(maxAttempts, 1);
    }

    @RabbitListener(queues = MqNames.DATAGEN_QUEUE,
            containerFactory = "datagenListenerContainerFactory")
    public void onDataGenTask(TaskMessage message,
                              Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                              @Header(name = MqNames.RETRY_LEVEL_HEADER, required = false) Integer retryLevel,
                              @Header(name = AmqpHeaders.REDELIVERED, required = false,
                                      defaultValue = "false") boolean redelivered) throws IOException {
        if (message == null || message.taskType() != TaskType.DATAGEN) {
            log.warn("造数据消息类型不匹配，丢弃: message={}", message);
            support.ack(channel, deliveryTag);
            return;
        }

        boolean allowInProgress = retryLevel != null || redelivered;
        DataGenTask task;
        try {
            task = stateService.claimById(message.taskId(), allowInProgress, maxAttempts);
            if (task == null && allowInProgress) {
                task = stateService.claimById(message.taskId(), false, maxAttempts);
            }
        } catch (RuntimeException exception) {
            log.error("领取造数据任务失败，等待重投: taskId={}", message.taskId(), exception);
            support.nack(channel, deliveryTag);
            return;
        }
        if (task == null) {
            support.ack(channel, deliveryTag);
            return;
        }

        try {
            dataGenWorker.process(task);
            support.ack(channel, deliveryTag);
        } catch (TaskRetryableException exception) {
            retryOrFail(message, task, exception, channel, deliveryTag);
        } catch (RuntimeException exception) {
            log.error("造数据消费异常: taskId={}", task.getId(), exception);
            retryOrFail(message, task,
                    new TaskRetryableException("system", "造数据任务执行异常，请稍后重试"),
                    channel, deliveryTag);
        }
    }

    private void retryOrFail(TaskMessage message,
                             DataGenTask task,
                             TaskRetryableException exception,
                             Channel channel,
                             long deliveryTag) throws IOException {
        int attempts = task.getAttempts() == null ? 1 : task.getAttempts();
        if (attempts < maxAttempts) {
            support.retry(message, attempts, channel, deliveryTag);
            return;
        }
        String stage = exception.getStage() == null ? "system" : exception.getStage();
        try {
            stateService.fail(task.getId(), stage, exception.getReason());
            publisher.publishDeadLetter(message);
        } catch (RuntimeException failException) {
            log.error("标记造数据任务失败状态异常，等待重投: taskId={}", task.getId(), failException);
            support.nack(channel, deliveryTag);
            return;
        }
        support.ack(channel, deliveryTag);
    }
}
