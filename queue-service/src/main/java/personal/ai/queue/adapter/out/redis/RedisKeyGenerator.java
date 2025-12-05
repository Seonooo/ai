package personal.ai.queue.adapter.out.redis;

/**
 * Redis Key 생성 유틸리티
 * Convention: {domain}:{type}:{identifier}
 */
public class RedisKeyGenerator {

    private static final String WAIT_QUEUE_PREFIX = "queue:wait:";
    private static final String ACTIVE_QUEUE_PREFIX = "queue:active:";
    private static final String ACTIVE_TOKEN_PREFIX = "active:token:";

    /**
     * Wait Queue Key
     * queue:wait:{concertId}
     */
    public static String waitQueueKey(String concertId) {
        return WAIT_QUEUE_PREFIX + concertId;
    }

    /**
     * Active Queue Key
     * queue:active:{concertId}
     */
    public static String activeQueueKey(String concertId) {
        return ACTIVE_QUEUE_PREFIX + concertId;
    }

    /**
     * Active Token Key (Hash)
     * active:token:{concertId}:{userId}
     */
    public static String activeTokenKey(String concertId, String userId) {
        return ACTIVE_TOKEN_PREFIX + concertId + ":" + userId;
    }

    /**
     * Wait Queue 패턴 (모든 콘서트)
     * queue:wait:*
     */
    public static String waitQueuePattern() {
        return WAIT_QUEUE_PREFIX + "*";
    }

    /**
     * Active Queue 패턴 (모든 콘서트)
     * queue:active:*
     */
    public static String activeQueuePattern() {
        return ACTIVE_QUEUE_PREFIX + "*";
    }

    /**
     * Key에서 Concert ID 추출
     */
    public static String extractConcertId(String key, String prefix) {
        if (key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        throw new IllegalArgumentException("Invalid key format: " + key);
    }
}
