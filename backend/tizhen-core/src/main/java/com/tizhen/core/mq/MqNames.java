package com.tizhen.core.mq;

import java.time.Duration;

/**
 * RabbitMQ 拓扑命名。队列与路由键集中在这里，生产端、消费端与测试共用，
 * 避免字符串散落导致声明与监听不一致。
 */
public final class MqNames {

    public static final String MAIN_EXCHANGE = "tizhen.task.exchange";
    public static final String RETRY_EXCHANGE = "tizhen.task.retry.exchange";

    public static final String JUDGE_OJ_QUEUE = "tizhen.judge.oj.queue";
    public static final String JUDGE_SHORT_ANSWER_QUEUE = "tizhen.judge.short_answer.queue";
    public static final String DATAGEN_QUEUE = "tizhen.datagen.queue";

    public static final String JUDGE_OJ_DLQ = "tizhen.judge.oj.dlq";
    public static final String JUDGE_SHORT_ANSWER_DLQ = "tizhen.judge.short_answer.dlq";
    public static final String DATAGEN_DLQ = "tizhen.datagen.dlq";

    /**
     * 重试消息头。正文只放 taskId 与 taskType，重试级别属于传输元数据。
     */
    public static final String RETRY_LEVEL_HEADER = "x-retry-level";

    private MqNames() {
    }

    public static String retryQueue(TaskType taskType, Duration delay) {
        return "tizhen." + taskType.routingKey() + ".retry." + delayLabel(delay);
    }

    public static String retryRoutingKey(TaskType taskType, Duration delay) {
        return "retry." + taskType.routingKey() + "." + delayLabel(delay);
    }

    public static String deadLetterRoutingKey(TaskType taskType) {
        return "dlq." + taskType.routingKey();
    }

    private static String delayLabel(Duration delay) {
        long millis = delay.toMillis();
        return millis % 1000 == 0
                ? (millis / 1000) + "s"
                : millis + "ms";
    }
}
