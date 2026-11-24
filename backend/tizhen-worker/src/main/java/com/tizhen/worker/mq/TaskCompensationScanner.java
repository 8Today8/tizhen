package com.tizhen.worker.mq;

import com.tizhen.core.datagen.entity.DataGenTask;
import com.tizhen.core.datagen.mapper.DataGenTaskMapper;
import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.mapper.SubmissionMapper;
import com.tizhen.core.mq.MqProperties;
import com.tizhen.core.mq.TaskMessage;
import com.tizhen.core.mq.TaskPublisher;
import com.tizhen.core.mq.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 补偿扫描：找回「已落库但超过阈值仍未进入执行态」的任务并重发。
 * Redis SETNX 保证同一任务在一个窗口内只补偿一次，避免每次扫描重复堆积消息。
 */
@Component
@ConditionalOnProperty(prefix = "tizhen.mq", name = "enabled", havingValue = "true")
public class TaskCompensationScanner {

    private static final Logger log = LoggerFactory.getLogger(TaskCompensationScanner.class);

    private static final String COMPENSATION_KEY_PREFIX = "tizhen:mq:compensate:";
    private static final int SCAN_LIMIT = 100;

    private final SubmissionMapper submissionMapper;
    private final DataGenTaskMapper dataGenTaskMapper;
    private final TaskPublisher publisher;
    private final StringRedisTemplate redisTemplate;
    private final MqProperties properties;

    public TaskCompensationScanner(SubmissionMapper submissionMapper,
                                   DataGenTaskMapper dataGenTaskMapper,
                                   TaskPublisher publisher,
                                   StringRedisTemplate redisTemplate,
                                   MqProperties properties) {
        this.submissionMapper = submissionMapper;
        this.dataGenTaskMapper = dataGenTaskMapper;
        this.publisher = publisher;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${tizhen.mq.scan-interval-ms:5000}")
    public void scan() {
        try {
            LocalDateTime threshold = LocalDateTime.now(ZoneOffset.UTC)
                    .minusSeconds(Math.max(properties.getStaleSeconds(), 1));
            List<Submission> submissions = submissionMapper.selectUndispatched(threshold, SCAN_LIMIT);
            for (Submission submission : submissions) {
                TaskType taskType = "oj".equals(submission.getProblemType())
                        ? TaskType.JUDGE_OJ
                        : TaskType.JUDGE_SHORT_ANSWER;
                publishOnce(new TaskMessage(submission.getId(), taskType));
            }
            List<DataGenTask> tasks = dataGenTaskMapper.selectPendingBefore(threshold, SCAN_LIMIT);
            for (DataGenTask task : tasks) {
                publishOnce(new TaskMessage(task.getId(), TaskType.DATAGEN));
            }
        } catch (RuntimeException exception) {
            log.warn("任务补偿扫描失败", exception);
        }
    }

    private void publishOnce(TaskMessage message) {
        String key = COMPENSATION_KEY_PREFIX
                + message.taskType().routingKey() + ":" + message.taskId();
        Duration ttl = Duration.ofSeconds(Math.max(properties.getStaleSeconds(), 30));
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }
        try {
            publisher.publish(message);
            log.info("补偿投递任务消息: taskId={}, taskType={}",
                    message.taskId(), message.taskType());
        } catch (RuntimeException exception) {
            // 释放去重键，下一轮扫描可再次尝试；不中断本轮其他任务的补偿。
            redisTemplate.delete(key);
            log.warn("补偿投递失败，等待下一轮扫描: taskId={}, taskType={}",
                    message.taskId(), message.taskType(), exception);
        }
    }
}
