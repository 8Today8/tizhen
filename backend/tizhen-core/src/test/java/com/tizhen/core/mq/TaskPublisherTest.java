package com.tizhen.core.mq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 发布器单元测试：不依赖 broker，验证交换机、路由键与重试级别的映射。
 */
class TaskPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private TaskPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new TaskPublisher(rabbitTemplate, new MqProperties());
    }

    @Test
    void publishSendsToMainExchangeWithTaskRoutingKey() {
        TaskMessage message = new TaskMessage(11L, TaskType.JUDGE_OJ);

        publisher.publish(message);

        MessagePostProcessor processor = verifySend(
                MqNames.MAIN_EXCHANGE, TaskType.JUDGE_OJ.routingKey(), message);
        Message amqpMessage = apply(processor);
        assertNull(amqpMessage.getMessageProperties().getHeader(MqNames.RETRY_LEVEL_HEADER));
    }

    @Test
    void retryLevelAdvancesWithFailedAttempts() {
        TaskMessage message = new TaskMessage(12L, TaskType.DATAGEN);

        publisher.publishRetry(message, 1);
        Message first = apply(verifySend(MqNames.RETRY_EXCHANGE,
                MqNames.retryRoutingKey(TaskType.DATAGEN, java.time.Duration.ofSeconds(5)),
                message));
        assertEquals(0, retryLevel(first));

        publisher.publishRetry(message, 2);
        Message second = apply(verifySend(MqNames.RETRY_EXCHANGE,
                MqNames.retryRoutingKey(TaskType.DATAGEN, java.time.Duration.ofSeconds(30)),
                message));
        assertEquals(1, retryLevel(second));
    }

    @Test
    void retryLevelStopsAtLastConfiguredDelay() {
        TaskMessage message = new TaskMessage(13L, TaskType.JUDGE_SHORT_ANSWER);

        publisher.publishRetry(message, 9);
        Message last = apply(verifySend(MqNames.RETRY_EXCHANGE,
                MqNames.retryRoutingKey(TaskType.JUDGE_SHORT_ANSWER,
                        java.time.Duration.ofSeconds(120)),
                message));
        assertEquals(2, retryLevel(last));
    }

    @Test
    void deadLetterGoesToDeadLetterRoutingKeyOnMainExchange() {
        TaskMessage message = new TaskMessage(14L, TaskType.DATAGEN);

        publisher.publishDeadLetter(message);

        verifySend(MqNames.MAIN_EXCHANGE, MqNames.deadLetterRoutingKey(TaskType.DATAGEN), message);
    }

    private MessagePostProcessor verifySend(String exchange, String routingKey,
                                            TaskMessage message) {
        ArgumentCaptor<MessagePostProcessor> processorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(routingKey), eq(message),
                processorCaptor.capture(), any(CorrelationData.class));
        return processorCaptor.getValue();
    }

    private int retryLevel(Message message) {
        return ((Number) message.getMessageProperties()
                .getHeader(MqNames.RETRY_LEVEL_HEADER)).intValue();
    }

    private Message apply(MessagePostProcessor processor) {
        try {
            return processor.postProcessMessage(new Message(new byte[0]));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
