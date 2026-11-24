package com.tizhen.core.mq;

/**
 * 任务落库后的领域事件。只在事务提交后转换为 MQ 消息，
 * 避免消息先于数据库可见导致消费端领取不到任务。
 */
public record TaskCreatedEvent(long taskId, TaskType taskType) {
}
