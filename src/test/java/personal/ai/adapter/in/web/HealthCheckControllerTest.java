package personal.ai.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaAdmin;
import personal.ai.adapter.in.web.dto.ApiResponse;
import personal.ai.adapter.in.web.dto.HealthCheckResponse;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthCheckController
 * Tests health check functionality with mocked infrastructure dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthCheckController Unit Tests")
class HealthCheckControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private KafkaAdmin kafkaAdmin;

    @Mock
    private Connection mockConnection;

    @Mock
    private RedisConnection mockRedisConnection;

    @InjectMocks
    private HealthCheckController healthCheckController;

    @Nested
    @DisplayName("Health Check - All Components Healthy")
    class AllComponentsHealthy {

        @BeforeEach
        void setUp() throws SQLException {
            // Database healthy
            when(dataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.isValid(anyInt())).thenReturn(true);

            // Redis healthy
            when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
                RedisCallback<?> callback = invocation.getArgument(0);
                return callback.doInRedis(mockRedisConnection);
            });
            when(mockRedisConnection.ping()).thenReturn("PONG");

            // Kafka healthy (admin exists)
            // kafkaAdmin is already mocked and non-null
        }

        @Test
        @DisplayName("Should return 200 OK with all services UP")
        void shouldReturnHealthyStatus() {
            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().result()).isEqualTo("success");
            assertThat(response.getBody().message()).isEqualTo("Application is healthy");

            HealthCheckResponse data = response.getBody().data();
            assertThat(data).isNotNull();
            assertThat(data.database()).isEqualTo("UP");
            assertThat(data.redis()).isEqualTo("UP");
            assertThat(data.kafka()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should verify database connection with 1 second timeout")
        void shouldVerifyDatabaseConnection() throws SQLException {
            // When
            healthCheckController.healthCheck();

            // Then
            verify(dataSource, times(1)).getConnection();
            verify(mockConnection, times(1)).isValid(1);
            verify(mockConnection, times(1)).close();
        }

        @Test
        @DisplayName("Should verify Redis connection with PING command")
        void shouldVerifyRedisConnection() {
            // When
            healthCheckController.healthCheck();

            // Then
            verify(redisTemplate, times(1)).execute(any(RedisCallback.class));
            verify(mockRedisConnection, times(1)).ping();
        }
    }

    @Nested
    @DisplayName("Health Check - Database Failures")
    class DatabaseFailures {

        @Test
        @DisplayName("Should return DOWN when database connection fails")
        void shouldReturnDownWhenDatabaseConnectionFails() throws SQLException {
            // Given
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));
            setupHealthyRedisAndKafka();

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().result()).isEqualTo("error");
            assertThat(response.getBody().message()).isEqualTo("Some components are unhealthy");
            assertThat(response.getBody().data().database()).isEqualTo("DOWN");
            assertThat(response.getBody().data().redis()).isEqualTo("UP");
            assertThat(response.getBody().data().kafka()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should return DOWN when connection isValid returns false")
        void shouldReturnDownWhenConnectionIsInvalid() throws SQLException {
            // Given
            when(dataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.isValid(anyInt())).thenReturn(false);
            setupHealthyRedisAndKafka();

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().database()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should handle SQLException during isValid check")
        void shouldHandleSQLExceptionDuringValidCheck() throws SQLException {
            // Given
            when(dataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.isValid(anyInt())).thenThrow(new SQLException("Timeout"));
            setupHealthyRedisAndKafka();

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().database()).isEqualTo("DOWN");
        }
    }

    @Nested
    @DisplayName("Health Check - Redis Failures")
    class RedisFailures {

        @Test
        @DisplayName("Should return DOWN when Redis ping fails")
        void shouldReturnDownWhenRedisPingFails() throws SQLException {
            // Given
            setupHealthyDatabase();
            when(redisTemplate.execute(any(RedisCallback.class)))
                    .thenThrow(new RuntimeException("Redis connection failed"));
            // Kafka healthy

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().result()).isEqualTo("error");
            assertThat(response.getBody().data().database()).isEqualTo("UP");
            assertThat(response.getBody().data().redis()).isEqualTo("DOWN");
            assertThat(response.getBody().data().kafka()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should return DOWN when Redis returns non-PONG response")
        void shouldReturnDownWhenRedisReturnsNonPong() throws SQLException {
            // Given
            setupHealthyDatabase();
            when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
                RedisCallback<?> callback = invocation.getArgument(0);
                return callback.doInRedis(mockRedisConnection);
            });
            when(mockRedisConnection.ping()).thenReturn("ERROR");
            // Kafka healthy

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().redis()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should return DOWN when Redis returns null")
        void shouldReturnDownWhenRedisReturnsNull() throws SQLException {
            // Given
            setupHealthyDatabase();
            when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);
            // Kafka healthy

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().redis()).isEqualTo("DOWN");
        }
    }

    @Nested
    @DisplayName("Health Check - Kafka Failures")
    class KafkaFailures {

        @Test
        @DisplayName("Should return DOWN when Kafka admin is null")
        void shouldReturnDownWhenKafkaAdminIsNull() throws SQLException {
            // Given
            setupHealthyDatabase();
            setupHealthyRedis();
            healthCheckController = new HealthCheckController(dataSource, redisTemplate, null);

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().result()).isEqualTo("error");
            assertThat(response.getBody().data().database()).isEqualTo("UP");
            assertThat(response.getBody().data().redis()).isEqualTo("UP");
            assertThat(response.getBody().data().kafka()).isEqualTo("DOWN");
        }
    }

    @Nested
    @DisplayName("Health Check - Multiple Component Failures")
    class MultipleComponentFailures {

        @Test
        @DisplayName("Should handle all components DOWN")
        void shouldHandleAllComponentsDown() throws SQLException {
            // Given
            when(dataSource.getConnection()).thenThrow(new SQLException());
            when(redisTemplate.execute(any(RedisCallback.class)))
                    .thenThrow(new RuntimeException());
            healthCheckController = new HealthCheckController(dataSource, redisTemplate, null);

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().result()).isEqualTo("error");
            assertThat(response.getBody().data().database()).isEqualTo("DOWN");
            assertThat(response.getBody().data().redis()).isEqualTo("DOWN");
            assertThat(response.getBody().data().kafka()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should handle database and Redis DOWN, Kafka UP")
        void shouldHandleDatabaseAndRedisDown() throws SQLException {
            // Given
            when(dataSource.getConnection()).thenThrow(new SQLException());
            when(redisTemplate.execute(any(RedisCallback.class)))
                    .thenThrow(new RuntimeException());
            // Kafka is healthy (non-null)

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().result()).isEqualTo("error");
            assertThat(response.getBody().data().database()).isEqualTo("DOWN");
            assertThat(response.getBody().data().redis()).isEqualTo("DOWN");
            assertThat(response.getBody().data().kafka()).isEqualTo("UP");
        }
    }

    @Nested
    @DisplayName("Health Check - Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle connection timeout gracefully")
        void shouldHandleConnectionTimeout() throws SQLException {
            // Given
            when(dataSource.getConnection()).thenAnswer(invocation -> {
                Thread.sleep(100); // Simulate slow connection
                return mockConnection;
            });
            when(mockConnection.isValid(anyInt())).thenReturn(true);
            setupHealthyRedisAndKafka();

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().database()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should not leave connections open on exception")
        void shouldCloseConnectionOnException() throws SQLException {
            // Given
            when(dataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.isValid(anyInt())).thenThrow(new SQLException());
            setupHealthyRedisAndKafka();

            // When
            healthCheckController.healthCheck();

            // Then
            verify(mockConnection, times(1)).close();
        }

        @Test
        @DisplayName("Should handle Redis callback exception")
        void shouldHandleRedisCallbackException() throws SQLException {
            // Given
            setupHealthyDatabase();
            when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
                RedisCallback<?> callback = invocation.getArgument(0);
                when(mockRedisConnection.ping()).thenThrow(new RuntimeException("Network error"));
                return callback.doInRedis(mockRedisConnection);
            });

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().redis()).isEqualTo("DOWN");
        }
    }

    @Nested
    @DisplayName("API Response Format Validation")
    class ApiResponseFormatValidation {

        @Test
        @DisplayName("Should return proper success response structure")
        void shouldReturnProperSuccessResponseStructure() throws SQLException {
            // Given
            setupHealthyDatabase();
            setupHealthyRedisAndKafka();

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode().value()).isEqualTo(200);

            ApiResponse<HealthCheckResponse> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.result()).isNotNull();
            assertThat(body.message()).isNotNull();
            assertThat(body.data()).isNotNull();
        }

        @Test
        @DisplayName("Should return proper error response structure")
        void shouldReturnProperErrorResponseStructure() throws SQLException {
            // Given
            when(dataSource.getConnection()).thenThrow(new SQLException());
            setupHealthyRedisAndKafka();

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode().value()).isEqualTo(200);

            ApiResponse<HealthCheckResponse> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.result()).isEqualTo("error");
            assertThat(body.message()).contains("unhealthy");
            assertThat(body.data()).isNotNull();
        }

        @Test
        @DisplayName("Should always return HTTP 200 regardless of health status")
        void shouldAlwaysReturn200() throws SQLException {
            // Given - all services down
            when(dataSource.getConnection()).thenThrow(new SQLException());
            when(redisTemplate.execute(any(RedisCallback.class)))
                    .thenThrow(new RuntimeException());
            healthCheckController = new HealthCheckController(dataSource, redisTemplate, null);

            // When
            ResponseEntity<ApiResponse<HealthCheckResponse>> response = healthCheckController.healthCheck();

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }
    }

    // Helper methods
    private void setupHealthyDatabase() throws SQLException {
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(anyInt())).thenReturn(true);
    }

    private void setupHealthyRedis() {
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            return callback.doInRedis(mockRedisConnection);
        });
        when(mockRedisConnection.ping()).thenReturn("PONG");
    }

    private void setupHealthyRedisAndKafka() {
        setupHealthyRedis();
        // Kafka is already mocked and non-null by @Mock annotation
    }
}