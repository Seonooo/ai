package personal.ai.queue.adapter.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import personal.ai.queue.application.port.in.CleanupExpiredTokensUseCase;
import personal.ai.queue.application.port.in.GetActiveConcertsUseCase;
import personal.ai.queue.application.port.in.MoveToActiveQueueUseCase;

import java.util.List;

/**
 * Queue Scheduler
 * Wait -> Active 전환 및 만료 토큰 정리를 주기적으로 실행
 * Virtual Thread 활용으로 Non-Blocking 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final MoveToActiveQueueUseCase moveToActiveQueueUseCase;
    private final CleanupExpiredTokensUseCase cleanupExpiredTokensUseCase;
    private final GetActiveConcertsUseCase getActiveConcertsUseCase;

    /**
     * Wait Queue -> Active Queue 전환 스케줄러
     * 주기: application.yml의 queue.scheduler.activation-interval-ms
     * 기본값: 5초
     */
    @Scheduled(fixedDelayString = "${queue.scheduler.activation-interval-ms:5000}")
    public void moveWaitingUsersToActive() {
        try {
            log.debug("Starting move scheduler");

            // 활성화된 콘서트 목록 조회
            List<String> concertIds = getActiveConcertsUseCase.getActiveConcerts();

            if (concertIds.isEmpty()) {
                log.debug("No active concerts found");
                return;
            }

            int totalMoved = 0;

            // 각 콘서트별로 처리
            for (String concertId : concertIds) {
                try {
                    int moved = moveToActiveQueueUseCase.moveWaitingToActive(concertId);
                    totalMoved += moved;

                    if (moved > 0) {
                        log.info("Moved users to active queue: concertId={}, count={}",
                                concertId, moved);
                    }
                } catch (Exception e) {
                    log.error("Failed to move users for concertId={}", concertId, e);
                }
            }

            if (totalMoved > 0) {
                log.info("Move scheduler completed: totalMoved={}, concerts={}",
                        totalMoved, concertIds.size());
            }

        } catch (Exception e) {
            log.error("Move scheduler failed", e);
        }
    }

    /**
     * 만료된 토큰 정리 스케줄러
     * 주기: application.yml의 queue.scheduler.cleanup-interval-ms
     * 기본값: 60초 (1분)
     */
    @Scheduled(fixedDelayString = "${queue.scheduler.cleanup-interval-ms:60000}")
    public void cleanupExpiredTokens() {
        try {
            log.debug("Starting cleanup scheduler");

            // 활성화된 콘서트 목록 조회
            List<String> concertIds = getActiveConcertsUseCase.getActiveConcerts();

            if (concertIds.isEmpty()) {
                log.debug("No active concerts found");
                return;
            }

            long totalRemoved = 0;

            // 각 콘서트별로 처리
            for (String concertId : concertIds) {
                try {
                    long removed = cleanupExpiredTokensUseCase.cleanupExpired(concertId);
                    totalRemoved += removed;

                    if (removed > 0) {
                        log.info("Cleaned up expired tokens: concertId={}, count={}",
                                concertId, removed);
                    }
                } catch (Exception e) {
                    log.error("Failed to cleanup tokens for concertId={}", concertId, e);
                }
            }

            if (totalRemoved > 0) {
                log.info("Cleanup scheduler completed: totalRemoved={}, concerts={}",
                        totalRemoved, concertIds.size());
            }

        } catch (Exception e) {
            log.error("Cleanup scheduler failed", e);
        }
    }
}
