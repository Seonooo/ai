package personal.ai.queue.domain.model;

/**
 * 대기열 상태
 * 유저의 대기열 진행 상태를 나타냄
 */
public enum QueueStatus {
    /**
     * 대기 중 (Wait Queue에 존재)
     */
    WAITING,

    /**
     * 입장 준비됨 (Active Queue로 전환됨, 아직 페이지 접속 전 - TTL 5분)
     */
    READY,

    /**
     * 활동 중 (페이지에 접속하여 예매 진행 중 - TTL 10분)
     */
    ACTIVE,

    /**
     * 만료됨 (TTL 초과 또는 결제 완료)
     */
    EXPIRED,

    /**
     * 존재하지 않음
     */
    NOT_FOUND
}
