package com.tizhen.core.mq;

/**
 * 可重试的任务执行失败。消费端根据尝试次数决定延迟重试还是标记失败；
 * V1 轮询路径收到该异常时保持原有行为，直接落失败状态。
 */
public class TaskRetryableException extends RuntimeException {

    private final String stage;
    private final String reason;

    public TaskRetryableException(String reason) {
        this(null, reason);
    }

    public TaskRetryableException(String stage, String reason) {
        super(reason);
        this.stage = stage;
        this.reason = reason;
    }

    public String getStage() {
        return stage;
    }

    public String getReason() {
        return reason;
    }
}
