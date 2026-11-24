package com.tizhen.worker.judge;

import com.rabbitmq.client.Channel;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.model.JudgeOutcome;
import com.tizhen.core.judge.service.JudgeResultService;
import com.tizhen.core.judge.service.SubmissionTaskService;
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
 * 判题消息消费者。消息只携带任务 id；领取、执行与终态回写复用
 * SubmissionTaskService / JudgeWorker，重复投递通过条件更新吸收。
 */
@Component
@ConditionalOnProperty(prefix = "tizhen.mq", name = "enabled", havingValue = "true")
public class JudgeMqConsumer {

    private static final Logger log = LoggerFactory.getLogger(JudgeMqConsumer.class);

    private final SubmissionTaskService taskService;
    private final JudgeResultService resultService;
    private final JudgeWorker judgeWorker;
    private final TaskPublisher publisher;
    private final TaskConsumerSupport support;
    private final int maxAttempts;

    public JudgeMqConsumer(SubmissionTaskService taskService,
                           JudgeResultService resultService,
                           JudgeWorker judgeWorker,
                           TaskPublisher publisher,
                           TaskConsumerSupport support,
                           @Value("${tizhen.judge.max-attempts:3}") int maxAttempts) {
        this.taskService = taskService;
        this.resultService = resultService;
        this.judgeWorker = judgeWorker;
        this.publisher = publisher;
        this.support = support;
        this.maxAttempts = Math.max(maxAttempts, 1);
    }

    @RabbitListener(queues = MqNames.JUDGE_OJ_QUEUE,
            containerFactory = "judgeListenerContainerFactory")
    public void onOjTask(TaskMessage message,
                         Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                         @Header(name = MqNames.RETRY_LEVEL_HEADER, required = false) Integer retryLevel,
                         @Header(name = AmqpHeaders.REDELIVERED, required = false,
                                 defaultValue = "false") boolean redelivered) throws IOException {
        handle(message, TaskType.JUDGE_OJ, channel, deliveryTag, retryLevel, redelivered);
    }

    @RabbitListener(queues = MqNames.JUDGE_SHORT_ANSWER_QUEUE,
            containerFactory = "judgeListenerContainerFactory")
    public void onShortAnswerTask(TaskMessage message,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                  @Header(name = MqNames.RETRY_LEVEL_HEADER, required = false) Integer retryLevel,
                                  @Header(name = AmqpHeaders.REDELIVERED, required = false,
                                          defaultValue = "false") boolean redelivered) throws IOException {
        handle(message, TaskType.JUDGE_SHORT_ANSWER, channel, deliveryTag, retryLevel, redelivered);
    }

    private void handle(TaskMessage message,
                        TaskType expectedType,
                        Channel channel,
                        long deliveryTag,
                        Integer retryLevel,
                        boolean redelivered) throws IOException {
        if (message == null || message.taskType() != expectedType) {
            log.warn("判题消息类型不匹配，丢弃: message={}, expected={}", message, expectedType);
            support.ack(channel, deliveryTag);
            return;
        }

        boolean allowInProgress = retryLevel != null || redelivered;
        Submission task;
        try {
            task = taskService.claimById(message.taskId(), allowInProgress, maxAttempts);
            if (task == null && allowInProgress) {
                // 任务可能已被超时恢复重置为 pending，退化为正常领取。
                task = taskService.claimById(message.taskId(), false, maxAttempts);
            }
        } catch (RuntimeException exception) {
            log.error("领取判题任务失败，等待重投: submissionId={}", message.taskId(), exception);
            support.nack(channel, deliveryTag);
            return;
        }
        if (task == null) {
            // 已处理、重复投递或达到尝试上限，直接确认。
            support.ack(channel, deliveryTag);
            return;
        }

        try {
            if (expectedType == TaskType.JUDGE_OJ) {
                judgeWorker.judgeOj(task);
            } else {
                judgeWorker.gradeShortAnswer(task);
            }
            support.ack(channel, deliveryTag);
        } catch (TaskRetryableException exception) {
            retryOrFail(message, task, exception, channel, deliveryTag);
        } catch (RuntimeException exception) {
            log.error("判题消费异常: submissionId={}", task.getId(), exception);
            retryOrFail(message, task,
                    new TaskRetryableException("判题失败，请稍后重新提交"),
                    channel, deliveryTag);
        }
    }

    private void retryOrFail(TaskMessage message,
                             Submission task,
                             TaskRetryableException exception,
                             Channel channel,
                             long deliveryTag) throws IOException {
        int attempts = task.getJudgeAttempts() == null ? 1 : task.getJudgeAttempts();
        if (attempts < maxAttempts) {
            support.retry(message, attempts, channel, deliveryTag);
            return;
        }
        try {
            boolean updated = resultService.complete(task.getId(),
                    JudgeOutcome.failed(exception.getReason()));
            if (updated) {
                publisher.publishDeadLetter(message);
            }
        } catch (RuntimeException completeException) {
            log.error("标记判题失败状态异常，等待重投: submissionId={}", task.getId(), completeException);
            support.nack(channel, deliveryTag);
            return;
        }
        support.ack(channel, deliveryTag);
    }
}
