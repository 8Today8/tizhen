package com.tizhen.core.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 任务消息发布器。发送失败不吞掉异常：初次投递由事务监听器记录后交给补偿扫描，
 * 重试投递由消费者转成 nack 重投，避免任务在重试阶段丢失。
 */
@Component
@ConditionalOnProperty(prefix = "tizhen.mq", name = "enabled", havingValue = "true")
public class TaskPublisher {

    private static final Logger log = LoggerFactory.getLogger(TaskPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MqProperties properties;

    public TaskPublisher(RabbitTemplate rabbitTemplate, MqProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        if (properties.getRetryDelays().isEmpty()) {
            throw new IllegalStateException("tizhen.mq.retry-delays 不能为空");
        }
    }

    public void publish(TaskMessage message) {
        send(MqNames.MAIN_EXCHANGE, message.taskType().routingKey(), message, null);
    }

    /**
     * 失败尝试次数从 1 开始。第 1 次失败进入第一级延迟队列，依次后移；
     * 超过配置的级别数时复用最后一级。
     */
    public void publishRetry(TaskMessage message, int failedAttempt) {
        List<Duration> delays = properties.getRetryDelays();
        int level = Math.min(Math.max(failedAttempt - 1, 0), delays.size() - 1);
        send(MqNames.RETRY_EXCHANGE,
                MqNames.retryRoutingKey(message.taskType(), delays.get(level)),
                message, level);
    }

    /**
     * 死信只做留存，投递失败只记日志，不影响数据库中的终态。
     */
    public void publishDeadLetter(TaskMessage message) {
        try {
            send(MqNames.MAIN_EXCHANGE,
                    MqNames.deadLetterRoutingKey(message.taskType()), message, null);
        } catch (RuntimeException exception) {
            log.warn("死信消息投递失败: taskId={}, taskType={}",
                    message.taskId(), message.taskType(), exception);
        }
    }

    private void send(String exchange, String routingKey, TaskMessage message, Integer retryLevel) {
        MessagePostProcessor processor = amqpMessage -> {
            amqpMessage.getMessageProperties().setMessageId(UUID.randomUUID().toString());
            if (retryLevel != null) {
                amqpMessage.getMessageProperties()
                        .setHeader(MqNames.RETRY_LEVEL_HEADER, retryLevel);
            }
            return amqpMessage;
        };
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, message, processor, correlation);
        correlation.getFuture().whenComplete((confirm, error) -> {
            if (error != null) {
                log.warn("任务消息发布确认异常: taskId={}, taskType={}",
                        message.taskId(), message.taskType(), error);
            } else if (confirm != null && !confirm.isAck()) {
                log.warn("任务消息发布被拒绝: taskId={}, taskType={}, reason={}",
                        message.taskId(), message.taskType(), confirm.getReason());
            }
        });
    }
}
