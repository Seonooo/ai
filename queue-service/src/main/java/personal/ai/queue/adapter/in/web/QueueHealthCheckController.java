package personal.ai.queue.adapter.in.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import personal.ai.common.dto.ApiResponse;
import personal.ai.common.dto.HealthCheckResponse;

/**
 * Queue Service Health Check Controller
 * Redis와 Kafka 상태만 확인 (경량 서비스)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QueueHealthCheckController {

    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaAdmin kafkaAdmin;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthCheckResponse>> healthCheck() {
        log.debug("Queue service health check requested");

        String redisStatus = checkRedis();
        String kafkaStatus = checkKafka();

        HealthCheckResponse data = HealthCheckResponse.forQueueService(redisStatus, kafkaStatus);

        boolean healthy = "UP".equals(redisStatus) && "UP".equals(kafkaStatus);

        if (healthy) {
            return ResponseEntity.ok(
                    ApiResponse.success("Queue service is healthy", data)
            );
        } else {
            return ResponseEntity.ok(
                    ApiResponse.error("Some components are unhealthy", data)
            );
        }
    }

    private String checkRedis() {
        try {
            String response = redisTemplate.execute((RedisConnection connection) -> {
                return connection.ping();
            });
            return "PONG".equals(response) ? "UP" : "DOWN";
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return "DOWN";
        }
    }

    private String checkKafka() {
        try {
            try (AdminClient adminClient =
                        AdminClient.create(kafkaAdmin.getConfigurationProperties())) {

                var clusterInfo = adminClient.describeCluster();
                var nodes = clusterInfo.nodes().get(5, java.util.concurrent.TimeUnit.SECONDS);

                return (nodes != null && !nodes.isEmpty()) ? "UP" : "DOWN";
            }
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            return "DOWN";
        }
    }
}
