package personal.ai.core.acceptance.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import personal.ai.core.booking.application.port.out.QueueServiceClient;

/**
 * Test용 QueueServiceClient Mock 설정
 * 실제 Queue Service 호출 없이 테스트 가능하도록 함
 */
@TestConfiguration
@Profile("test")
public class TestQueueServiceClientConfig {

    @Bean
    @Primary
    public QueueServiceClient mockQueueServiceClient() {
        return (userId, queueToken) -> {
            // 테스트에서는 항상 검증 통과
            // Do nothing - always pass
        };
    }
}
