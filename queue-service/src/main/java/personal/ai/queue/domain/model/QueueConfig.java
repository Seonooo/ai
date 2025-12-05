package personal.ai.queue.domain.model;

/**
 * 대기열 설정 (Value Object)
 * 비즈니스 정책을 담는 설정 객체
 */
public record QueueConfig(
        int activeMaxSize,
        int tokenTtlSeconds,
        int activatedTtlSeconds,
        int maxExtensionCount
) {
    /**
     * 기본 설정 (대규모 트래픽 대비)
     */
    public static QueueConfig defaultConfig() {
        return new QueueConfig(
                50000,  // 동시 처리 50,000명
                300,    // 진입 대기 5분 (Ready 상태)
                600,    // 활동 보장 10분 (Active 상태)
                2       // 최대 연장 2회
        );
    }

    /**
     * 커스텀 설정
     */
    public static QueueConfig of(int activeMaxSize, int tokenTtlSeconds) {
        return new QueueConfig(
                activeMaxSize,
                tokenTtlSeconds,
                600,    // 활동 보장 10분 (고정)
                2       // 최대 연장 2회 (고정)
        );
    }
}
