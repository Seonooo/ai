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
                Integer estimatedWaitMinutes,
                String status,
                String token,
                boolean isNewEntry) {
        // 상수
        private static final int SECONDS_PER_MINUTE = 60;

        /**
         * 신규 진입 대기열 정보 생성
         */
        public static QueuePosition newEntry(
                        String concertId,
                        String userId,
                        Long position,
                        Long totalWaiting,
                        int activeCapacity,
                        int processIntervalSeconds) {
                return create(concertId, userId, position, totalWaiting, activeCapacity, processIntervalSeconds,
                                QueueStatus.WAITING, null, true);
        }

        /**
         * 이미 대기 중인 대기열 정보 생성
         */
        public static QueuePosition alreadyWaiting(
                        String concertId,
                        String userId,
                        Long position,
                        Long totalWaiting,
                        int activeCapacity,
                        int processIntervalSeconds) {
                return create(concertId, userId, position, totalWaiting, activeCapacity, processIntervalSeconds,
                                QueueStatus.WAITING, null, false);
        }

        /**
         * 이미 활성화된 상태 정보 생성
         */
        public static QueuePosition alreadyActive(QueueToken token) {
                return new QueuePosition(
                                token.concertId(),
                                token.userId(),
                                0L, // Position 0 for active users
                                0L,
                                0, // 0 minutes wait
                                token.status().name(),
                                token.token(),
                                false);
        }

        private static QueuePosition create(
                        String concertId,
                        String userId,
                        Long position,
                        Long totalWaiting,
                        int activeCapacity,
                        int processIntervalSeconds,
                        QueueStatus status,
                        String token,
                        boolean isNewEntry) {
                // 예상 대기 시간 계산
                // (순번 / 동시처리인원) * 활성화주기 / 60 = 분 단위
                int estimatedMinutes = (int) Math.ceil(
                                (double) position / activeCapacity * processIntervalSeconds / SECONDS_PER_MINUTE);

                return new QueuePosition(
                                concertId,
                                userId,
                                position,
                                totalWaiting,
                                estimatedMinutes,
                                status.name(),
                                token,
                                isNewEntry);
        }
}
