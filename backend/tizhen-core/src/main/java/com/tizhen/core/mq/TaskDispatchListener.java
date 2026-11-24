package com.tizhen.core.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 事务提交后把任务事件转换为 MQ 消息。发布失败只记日志，
 * 任务仍留在数据库 pending 状态，由补偿扫描重发。
 */
@Component
@ConditionalOnProperty(prefix = "tizhen.mq", name = "enabled", havingValue = "true")
public class TaskDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(TaskDispatchListener.class);

    private final TaskPublisher publisher;

    public TaskDispatchListener(TaskPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreated(TaskCreatedEvent event) {
        try {
            publisher.publish(new TaskMessage(event.taskId(), event.taskType()));
        } catch (RuntimeException exception) {
            log.warn("任务消息发布失败，等待补偿扫描: taskId={}, taskType={}",
                    event.taskId(), event.taskType(), exception);
        }
    }
}
