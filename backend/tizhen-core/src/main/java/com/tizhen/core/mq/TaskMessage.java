package com.tizhen.core.mq;

/**
 * 任务投递消息。只携带任务 ID 与类型，业务状态始终以数据库为准，
 * 消费端收到后仍需按 id + 状态条件领取，天然支持重复投递。
 */
public record TaskMessage(long taskId, TaskType taskType) {
}
