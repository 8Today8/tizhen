package com.tizhen.core.mq;

/**
 * 异步任务类型与对应队列。任务类型决定主队列、路由键与死信队列。
 */
public enum TaskType {

    JUDGE_OJ(MqNames.JUDGE_OJ_QUEUE, MqNames.JUDGE_OJ_DLQ, "judge.oj"),
    JUDGE_SHORT_ANSWER(MqNames.JUDGE_SHORT_ANSWER_QUEUE, MqNames.JUDGE_SHORT_ANSWER_DLQ, "judge.short_answer"),
    DATAGEN(MqNames.DATAGEN_QUEUE, MqNames.DATAGEN_DLQ, "datagen");

    private final String queue;
    private final String deadLetterQueue;
    private final String routingKey;

    TaskType(String queue, String deadLetterQueue, String routingKey) {
        this.queue = queue;
        this.deadLetterQueue = deadLetterQueue;
        this.routingKey = routingKey;
    }

    public String queue() {
        return queue;
    }

    public String deadLetterQueue() {
        return deadLetterQueue;
    }

    public String routingKey() {
        return routingKey;
    }
}
