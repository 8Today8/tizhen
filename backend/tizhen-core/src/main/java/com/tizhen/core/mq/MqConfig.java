package com.tizhen.core.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务消息拓扑：主队列按任务类型拆分，重试通过独立的 TTL 队列死信回主交换机，
 * 死信队列只留存不消费。拓扑在 app 与 worker 中均可重复声明。
 */
@Configuration
@EnableConfigurationProperties(MqProperties.class)
@ConditionalOnProperty(prefix = "tizhen.mq", name = "enabled", havingValue = "true")
public class MqConfig {

    private static final Logger log = LoggerFactory.getLogger(MqConfig.class);

    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(MqNames.MAIN_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange taskRetryExchange() {
        return new DirectExchange(MqNames.RETRY_EXCHANGE, true, false);
    }

    @Bean
    public Declarables taskDeclarables(MqProperties properties) {
        List<Declarable> declarables = new ArrayList<>();
        for (TaskType taskType : TaskType.values()) {
            Queue queue = QueueBuilder.durable(taskType.queue())
                    .deadLetterExchange(MqNames.MAIN_EXCHANGE)
                    .deadLetterRoutingKey(MqNames.deadLetterRoutingKey(taskType))
                    .build();
            Queue deadLetterQueue = QueueBuilder.durable(taskType.deadLetterQueue()).build();
            declarables.add(queue);
            declarables.add(deadLetterQueue);
            declarables.add(BindingBuilder.bind(queue)
                    .to(taskExchange())
                    .with(taskType.routingKey()));
            declarables.add(BindingBuilder.bind(deadLetterQueue)
                    .to(taskExchange())
                    .with(MqNames.deadLetterRoutingKey(taskType)));

            for (Duration delay : properties.getRetryDelays()) {
                Queue retryQueue = QueueBuilder.durable(MqNames.retryQueue(taskType, delay))
                        .withArgument("x-message-ttl", delay.toMillis())
                        .deadLetterExchange(MqNames.MAIN_EXCHANGE)
                        .deadLetterRoutingKey(taskType.routingKey())
                        .build();
                declarables.add(retryQueue);
                declarables.add(BindingBuilder.bind(retryQueue)
                        .to(taskRetryExchange())
                        .with(MqNames.retryRoutingKey(taskType, delay)));
            }
        }
        return new Declarables(declarables);
    }

    /**
     * 关闭 Spring Boot 默认的 RabbitAdmin（spring.rabbitmq.dynamic=false），
     * 改由本 Bean 在开关开启时声明拓扑，避免关闭 MQ 时启动仍尝试连接 broker。
     */
    @Bean
    public RabbitAdmin taskRabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 消息不可路由时记录日志；发布确认失败由 TaskPublisher 记录，最终依赖补偿扫描兜底。
     */
    @Bean
    public RabbitTemplateCustomizer taskRabbitTemplateCustomizer() {
        return template -> {
            template.setMandatory(true);
            template.setReturnsCallback(returned -> log.warn(
                    "任务消息不可路由: exchange={}, routingKey={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        };
    }
}
