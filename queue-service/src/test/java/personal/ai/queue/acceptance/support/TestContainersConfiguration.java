package personal.ai.queue.acceptance.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Queue Service Testcontainers 설정 클래스
 * 실제 Redis, Kafka 컨테이너를 사용하여 통합 테스트 수행
 * agent.md Testing Strategy - Testcontainers 활용
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    /**
     * Redis 컨테이너
     * @ServiceConnection을 사용하여 자동으로 Redis 설정
     * Queue Service의 핵심 저장소
     */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);  // 재사용으로 테스트 속도 향상
    }

    /**
     * Kafka 컨테이너
     * @ServiceConnection을 사용하여 자동으로 Kafka 설정
     * 이벤트 발행/구독용
     */
    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
                .withReuse(true);
    }
}
