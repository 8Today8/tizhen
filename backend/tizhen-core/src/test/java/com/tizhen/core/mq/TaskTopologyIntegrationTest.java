package com.tizhen.core.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tizhen.testsupport.RabbitMqTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 拓扑与投递集成测试：验证主队列、TTL 重试队列、死信队列的声明与路由，
 * 以及 TaskPublisher 的消息格式。无 Docker 时由基类自动跳过。
 */
class TaskTopologyIntegrationTest extends RabbitMqTestBase {

    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);

    private static CachingConnectionFactory connectionFactory;
    private static RabbitAdmin admin;
    private static RabbitTemplate template;
    private static MessageConverter converter;
    private static TaskPublisher publisher;

    @BeforeAll
    static void setUp() {
        connectionFactory = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
        connectionFactory.setUsername(RABBIT.getAdminUsername());
        connectionFactory.setPassword(RABBIT.getAdminPassword());
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);

        MqProperties properties = new MqProperties();
        properties.setRetryDelays(List.of(RETRY_DELAY));

        MqConfig config = new MqConfig();
        converter = config.rabbitMessageConverter(new ObjectMapper());

        template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        template.setReceiveTimeout(5_000);
        publisher = new TaskPublisher(template, properties);

        admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(false);
        admin.declareExchange(config.taskExchange());
        admin.declareExchange(config.taskRetryExchange());
        for (Declarable declarable : config.taskDeclarables(properties).getDeclarables()) {
            if (declarable instanceof Exchange exchange) {
                admin.declareExchange(exchange);
            } else if (declarable instanceof Queue queue) {
                admin.declareQueue(queue);
            } else if (declarable instanceof Binding binding) {
                admin.declareBinding(binding);
            }
        }
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void purgeQueues() {
        for (TaskType taskType : TaskType.values()) {
            admin.purgeQueue(taskType.queue(), false);
            admin.purgeQueue(taskType.deadLetterQueue(), false);
            admin.purgeQueue(MqNames.retryQueue(taskType, RETRY_DELAY), false);
        }
    }

    @Test
    void declaresMainRetryAndDeadLetterQueues() {
        for (TaskType taskType : TaskType.values()) {
            assertNotNull(admin.getQueueProperties(taskType.queue()),
                    "主队列未声明: " + taskType.queue());
            assertNotNull(admin.getQueueProperties(taskType.deadLetterQueue()),
                    "死信队列未声明: " + taskType.deadLetterQueue());
            assertNotNull(admin.getQueueProperties(
                            MqNames.retryQueue(taskType, RETRY_DELAY)),
                    "重试队列未声明: " + taskType);
        }
    }

    @Test
    void publishesTaskMessageToMainQueue() {
        TaskMessage message = new TaskMessage(101L, TaskType.JUDGE_OJ);
        publisher.publish(message);

        Message raw = receive(TaskType.JUDGE_OJ.queue());
        assertNotNull(raw);
        assertEquals(message, converter.fromMessage(raw));
    }

    @Test
    void retryMessageReturnsToMainQueueAfterTtl() {
        TaskMessage message = new TaskMessage(102L, TaskType.JUDGE_OJ);
        publisher.publishRetry(message, 1);

        Message raw = receive(TaskType.JUDGE_OJ.queue());
        assertNotNull(raw);
        assertEquals(message, converter.fromMessage(raw));
        Object retryLevel = raw.getMessageProperties()
                .getHeader(MqNames.RETRY_LEVEL_HEADER);
        assertNotNull(retryLevel);
        assertEquals(0, ((Number) retryLevel).intValue());
    }

    @Test
    void deadLetterMessageRoutesToDlq() {
        TaskMessage message = new TaskMessage(103L, TaskType.DATAGEN);
        publisher.publishDeadLetter(message);

        Message raw = receive(TaskType.DATAGEN.deadLetterQueue());
        assertNotNull(raw);
        assertEquals(message, converter.fromMessage(raw));
    }

    private Message receive(String queue) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            Message message = receiveOnce(queue);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    private Message receiveOnce(String queue) {
        return template.receive(queue, 300);
    }
}
