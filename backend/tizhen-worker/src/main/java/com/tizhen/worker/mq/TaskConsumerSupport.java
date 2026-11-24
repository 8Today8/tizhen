package com.tizhen.worker.mq;

import com.rabbitmq.client.Channel;
import com.tizhen.core.mq.TaskMessage;
import com.tizhen.core.mq.TaskPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 消费者 ack/nack 与重试投递的公共逻辑。重试投递失败时 nack 重投，
 * 让 broker 保留消息；任务状态仍由数据库事实源决定。
 */
@Component
@ConditionalOnProperty(prefix = "tizhen.mq", name = "enabled", havingValue = "true")
public class TaskConsumerSupport {

    private static final Logger log = LoggerFactory.getLogger(TaskConsumerSupport.class);

    private final TaskPublisher publisher;

    public TaskConsumerSupport(TaskPublisher publisher) {
        this.publisher = publisher;
    }

    public void ack(Channel channel, long deliveryTag) throws IOException {
        channel.basicAck(deliveryTag, false);
    }

    public void nack(Channel channel, long deliveryTag) throws IOException {
        channel.basicNack(deliveryTag, false, true);
    }

    /**
     * 投递下一级延迟重试并 ack；投递失败则 nack，由 broker 立即重投。
     */
    public void retry(TaskMessage message, int failedAttempt, Channel channel, long deliveryTag)
            throws IOException {
        try {
            publisher.publishRetry(message, failedAttempt);
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            log.warn("重试消息投递失败，nack 等待 broker 重投: taskId={}, taskType={}",
                    message.taskId(), message.taskType(), exception);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
