package personal.ai.queue.domain.model;

/**
 * 대기열 설정 (Value Object)
 * 비즈니스 정책을 담는 설정 객체
 */
public record QueueConfig(
        int activeMaxSize,
        int tokenTtlSeconds,
        int activatedTtlSeconds,
        int maxExtensionCount,
        int activationIntervalSeconds  // Wait -> Active 전환 주기 (초)
) {
    // 기본값 상수
    private static final int DEFAULT_ACTIVE_MAX_SIZE = 50000;
    private static final int DEFAULT_TOKEN_TTL_SECONDS = 300;  // 5분
    private static final int DEFAULT_ACTIVATED_TTL_SECONDS = 600;  // 10분
    private static final int DEFAULT_MAX_EXTENSION_COUNT = 2;
    private static final int DEFAULT_ACTIVATION_INTERVAL_SECONDS = 5;

    /**
     * 기본 설정 (대규모 트래픽 대비)
     */
    public static QueueConfig defaultConfig() {
        return new QueueConfig(
                DEFAULT_ACTIVE_MAX_SIZE,
                DEFAULT_TOKEN_TTL_SECONDS,
                DEFAULT_ACTIVATED_TTL_SECONDS,
                DEFAULT_MAX_EXTENSION_COUNT,
                DEFAULT_ACTIVATION_INTERVAL_SECONDS
        );
    }

    /**
     * 커스텀 설정
     */
    public static QueueConfig of(int activeMaxSize, int tokenTtlSeconds, int activationIntervalSeconds) {
        return new QueueConfig(
                activeMaxSize,
                tokenTtlSeconds,
                DEFAULT_ACTIVATED_TTL_SECONDS,
                DEFAULT_MAX_EXTENSION_COUNT,
                activationIntervalSeconds
        );
    }
}
