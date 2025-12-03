package personal.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AiApplication main class
 * Verifies Spring Boot application context loads correctly
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AiApplication Unit Tests")
class AiApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Should load application context successfully")
    void shouldLoadApplicationContext() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("Should have HealthCheckController bean")
    void shouldHaveHealthCheckControllerBean() {
        assertThat(applicationContext.containsBean("healthCheckController")).isTrue();
    }

    @Test
    @DisplayName("Should configure DataSource bean")
    void shouldConfigureDataSourceBean() {
        assertThat(applicationContext.containsBean("dataSource")).isTrue();
    }

    @Test
    @DisplayName("Should configure RedisTemplate bean")
    void shouldConfigureRedisTemplateBean() {
        boolean hasRedisTemplate = applicationContext.getBeanNamesForType(
                org.springframework.data.redis.core.RedisTemplate.class).length > 0;
        assertThat(hasRedisTemplate).isTrue();
    }

    @Test
    @DisplayName("Should configure KafkaAdmin bean")
    void shouldConfigureKafkaAdminBean() {
        assertThat(applicationContext.containsBean("kafkaAdmin")).isTrue();
    }

    @Test
    @DisplayName("Should have Spring Boot application annotation")
    void shouldHaveSpringBootApplicationAnnotation() {
        assertThat(AiApplication.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.SpringBootApplication.class)).isTrue();
    }
}