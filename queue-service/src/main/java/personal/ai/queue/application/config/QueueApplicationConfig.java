package personal.ai.queue.application.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import personal.ai.queue.domain.model.QueueConfig;
import personal.ai.queue.domain.service.QueueDomainService;

/**
 * Queue Application Layer Configuration
 */
@Configuration
@EnableConfigurationProperties(QueueConfigProperties.class)
public class QueueApplicationConfig {

    @Bean
    public QueueConfig queueConfig(QueueConfigProperties properties) {
        return QueueConfig.of(
                properties.active().maxSize(),
                properties.active().tokenTtlSeconds()
        );
    }

    @Bean
    public QueueDomainService queueDomainService(QueueConfig queueConfig) {
        return new QueueDomainService(queueConfig);
    }
}
