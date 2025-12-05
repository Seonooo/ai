package personal.ai.queue.domain.model;

/**
 * 대기열 순번 정보 (Value Object)
 * 대기 중인 유저의 순번과 예상 대기 시간 정보
 */
public record QueuePosition(
        String concertId,
        String userId,
        Long position,
        Long totalWaiting,
        Integer estimatedWaitMinutes
) {
    /**
     * 예상 대기 시간 계산
     * @param concertId 콘서트 ID
     * @param userId 사용자 ID
     * @param position 현재 순번
     * @param totalWaiting 전체 대기 인원
     * @param activeCapacity 동시 처리 가능 인원
     * @param processIntervalSeconds 활성화 주기 (초)
     */
    public static QueuePosition calculate(
            String concertId,
            String userId,
            Long position,
            Long totalWaiting,
            int activeCapacity,
            int processIntervalSeconds
    ) {
        // 예상 대기 시간 계산
        // (순번 / 동시처리인원) * 활성화주기 / 60 = 분 단위
        int estimatedMinutes = (int) Math.ceil(
                (double) position / activeCapacity * processIntervalSeconds / 60
        );

        return new QueuePosition(
                concertId,
                userId,
                position,
                totalWaiting,
                estimatedMinutes
        );
    }
}
