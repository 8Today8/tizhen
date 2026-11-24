package com.tizhen.worker.mq;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

/**
 * 消费者容器配置。手动 ack 保证「业务回写成功才确认」；prefetch 与消费并发对齐，
 * 判题与造数据使用各自独立并发度。
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "tizhen.mq", name = "enabled", havingValue = "true")
public class MqListenerConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory judgeListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            @Value("${worker.judge-concurrency:4}") int concurrency) {
        return createFactory(connectionFactory, messageConverter, concurrency);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory datagenListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            @Value("${worker.datagen-concurrency:2}") int concurrency) {
        return createFactory(connectionFactory, messageConverter, concurrency);
    }

    private SimpleRabbitListenerContainerFactory createFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter, int concurrency) {
        int consumers = Math.max(concurrency, 1);
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(consumers);
        factory.setConcurrentConsumers(consumers);
        factory.setMaxConcurrentConsumers(consumers);
        factory.setDefaultRequeueRejected(false);
        factory.setMissingQueuesFatal(false);
        return factory;
    }
}
