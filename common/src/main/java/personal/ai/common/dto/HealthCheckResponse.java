package personal.ai.common.dto;

/**
 * Health Check 응답 데이터
 *
 * @param database 데이터베이스 상태 ("UP" 또는 "DOWN") - Core Service만
 * @param redis    Redis 상태 ("UP" 또는 "DOWN")
 * @param kafka    Kafka 상태 ("UP" 또는 "DOWN")
 */
public record HealthCheckResponse(
        String database,
        String redis,
        String kafka
) {
    /**
     * Core Service용 생성자 (database, redis, kafka)
     */
    public HealthCheckResponse(String database, String redis, String kafka) {
        this.database = database;
        this.redis = redis;
        this.kafka = kafka;
    }

    /**
     * Queue Service용 생성자 (redis, kafka만)
     */
    public static HealthCheckResponse forQueueService(String redis, String kafka) {
        return new HealthCheckResponse(null, redis, kafka);
    }
}
