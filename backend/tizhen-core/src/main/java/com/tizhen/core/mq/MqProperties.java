package com.tizhen.core.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 异步消息配置。关闭开关时只走数据库轮询，不连接 broker。
 */
@ConfigurationProperties(prefix = "tizhen.mq")
public class MqProperties {

    /** 总开关：关闭时保持 V1 数据库轮询，不声明队列、不连接 RabbitMQ。 */
    private boolean enabled;

    /** 超过该时长仍未领取的任务视为可能丢消息，由补偿扫描重发。 */
    private long staleSeconds = 30;

    /** 补偿扫描周期。 */
    private long scanIntervalMs = 5_000;

    /** 重试延迟级别，与 TTL 重试队列一一对应。 */
    private List<Duration> retryDelays = List.of(
            Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofSeconds(120));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getStaleSeconds() {
        return staleSeconds;
    }

    public void setStaleSeconds(long staleSeconds) {
        this.staleSeconds = staleSeconds;
    }

    public long getScanIntervalMs() {
        return scanIntervalMs;
    }

    public void setScanIntervalMs(long scanIntervalMs) {
        this.scanIntervalMs = scanIntervalMs;
    }

    public List<Duration> getRetryDelays() {
        return retryDelays;
    }

    public void setRetryDelays(List<Duration> retryDelays) {
        this.retryDelays = retryDelays == null || retryDelays.isEmpty()
                ? List.of(Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofSeconds(120))
                : retryDelays;
    }
}
