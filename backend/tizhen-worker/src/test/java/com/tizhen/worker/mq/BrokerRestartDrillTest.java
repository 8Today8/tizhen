package com.tizhen.worker.mq;

import com.tizhen.core.judge.entity.Submission;
import com.tizhen.core.judge.service.JudgeResultService;
import com.tizhen.core.judge.service.SubmissionTaskService;
import com.tizhen.core.mq.MqConfig;
import com.tizhen.core.mq.MqNames;
import com.tizhen.core.mq.TaskMessage;
import com.tizhen.core.mq.TaskPublisher;
import com.tizhen.core.mq.TaskType;
import com.tizhen.worker.judge.JudgeMqConsumer;
import com.tizhen.worker.judge.JudgeWorker;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * broker 重启故障演练：broker 停止期间连接与健康检查不可用，
 * 重启后发布端自动重连、持久化队列与绑定保留、消费者恢复并继续 ack，
 * 期间发布失败的消息由数据库状态与补偿扫描兜底（见 BusinessChainIntegrationTest）。
 */
@SpringBootTest(classes = BrokerRestartDrillTest.TestApplication.class,
        properties = {
                "spring.main.web-application-type=none",
                "spring.rabbitmq.dynamic=false",
                "spring.rabbitmq.connection-timeout=3s",
                "tizhen.mq.enabled=true",
                "tizhen.mq.retry-delays=1s",
                "tizhen.mq.scan-interval-ms=600000",
                "tizhen.judge.max-attempts=3",
                "worker.judge-concurrency=1"
        })
@Testcontainers(disabledWithoutDocker = true)
class BrokerRestartDrillTest {

    /**
     * 固定宿主端口：Docker 重启容器时会重新分配随机端口，
     * 只有显式绑定才能保证重启后 Spring 连接配置仍然有效。
     * 注意必须覆盖容器暴露的全部端口：Testcontainers 启动后会校验每个暴露端口都有映射，
     * 只绑定 5672 会导致该校验超时失败。
     */
    private static final Set<Integer> RESERVED_PORTS = new HashSet<>();
    private static final int RABBIT_HOST_PORT = findFreePort();

    @Container
    private static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"))
                    .withAdminUser("tizhen")
                    .withAdminPassword("tizhen_test")
                    .withCreateContainerCmdModifier(command -> command.getHostConfig()
                            .withPortBindings(
                                    binding(5672, RABBIT_HOST_PORT),
                                    binding(5671, findFreePort()),
                                    binding(15672, findFreePort()),
                                    binding(15671, findFreePort())));

    @DynamicPropertySource
    static void registerRabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", () -> "127.0.0.1");
        registry.add("spring.rabbitmq.port", () -> RABBIT_HOST_PORT);
        registry.add("spring.rabbitmq.username", () -> "tizhen");
        registry.add("spring.rabbitmq.password", () -> "tizhen_test");
    }

    @Autowired
    private TaskPublisher publisher;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private SubmissionTaskService taskService;
    @Autowired
    private JudgeResultService resultService;
    @Autowired
    private JudgeWorker judgeWorker;

    @BeforeEach
    void resetState() {
        Mockito.reset(taskService, resultService, judgeWorker);
    }

    @Test
    void brokerRestartKeepsTopologyAndConsumersRecover() {
        when(taskService.claimById(1L, false, 3)).thenReturn(submission(1L, 1));
        when(taskService.claimById(2L, false, 3)).thenReturn(submission(2L, 1));

        publisher.publish(new TaskMessage(1L, TaskType.JUDGE_OJ));
        verify(judgeWorker, timeout(10_000)).judgeOj(any());

        RABBIT.stop();
        assertThrows(AmqpException.class, () -> rabbitTemplate.execute(channel -> {
            channel.queueDeclarePassive(MqNames.JUDGE_OJ_QUEUE);
            return true;
        }), "broker 停止后连接应不可用");

        RABBIT.start();
        awaitQueueAvailable(MqNames.JUDGE_OJ_QUEUE, Duration.ofSeconds(30));

        publishEventually(new TaskMessage(2L, TaskType.JUDGE_OJ), Duration.ofSeconds(30));

        verify(judgeWorker, timeout(30_000).times(2)).judgeOj(any());
    }

    /**
     * broker 重启后发布端可能需要若干次连接重建才能成功，失败只重试不抛错。
     */
    private void publishEventually(TaskMessage message, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        RuntimeException lastError = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                publisher.publish(message);
                return;
            } catch (RuntimeException exception) {
                lastError = exception;
                sleep(500);
            }
        }
        throw new AssertionError("broker 重启后消息仍无法投递: " + message, lastError);
    }

    private void awaitQueueAvailable(String queue, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        RuntimeException lastError = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                rabbitTemplate.execute(channel -> {
                    channel.queueDeclarePassive(queue);
                    return true;
                });
                return;
            } catch (RuntimeException exception) {
                lastError = exception;
                sleep(500);
            }
        }
        throw new AssertionError("broker 重启后队列未恢复: " + queue, lastError);
    }

    private Submission submission(long id, int attempts) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setProblemType("oj");
        submission.setJudgeAttempts(attempts);
        return submission;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static PortBinding binding(int containerPort, int hostPort) {
        return new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(containerPort));
    }

    private static synchronized int findFreePort() {
        while (true) {
            try (ServerSocket socket = new ServerSocket(0)) {
                int port = socket.getLocalPort();
                if (RESERVED_PORTS.add(port)) {
                    return port;
                }
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({MqConfig.class, TaskPublisher.class, MqListenerConfig.class,
            TaskConsumerSupport.class, JudgeMqConsumer.class})
    static class TestApplication {

        @Bean
        SubmissionTaskService submissionTaskService() {
            return mock(SubmissionTaskService.class);
        }

        @Bean
        JudgeResultService judgeResultService() {
            return mock(JudgeResultService.class);
        }

        @Bean
        JudgeWorker judgeWorker() {
            return mock(JudgeWorker.class);
        }
    }
}
