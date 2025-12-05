package personal.ai.queue.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import personal.ai.queue.application.port.in.CleanupExpiredTokensUseCase;
import personal.ai.queue.application.port.in.MoveToActiveQueueUseCase;
import personal.ai.queue.application.port.out.QueueRepository;
import personal.ai.queue.domain.model.QueueConfig;
import personal.ai.queue.domain.service.QueueDomainService;

import java.time.Instant;
import java.util.List;

/**
 * Queue Scheduler Service
 * Wait -> Active 전환 및 만료 토큰 정리 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueSchedulerService implements
        MoveToActiveQueueUseCase,
        CleanupExpiredTokensUseCase {

    private final QueueRepository queueRepository;
    private final QueueDomainService domainService;
    private final QueueConfig queueConfig;

    @Override
    public int moveWaitingToActive(String concertId) {
        log.debug("Moving users from wait to active queue: concertId={}", concertId);

        // 현재 Active Queue 크기 확인
        Long currentActiveSize = queueRepository.getActiveQueueSize(concertId);

        // 전환 가능한 인원 계산
        int availableSlots = domainService.calculateBatchSize(currentActiveSize);

        if (availableSlots <= 0) {
            log.debug("No available slots: concertId={}, currentSize={}",
                    concertId, currentActiveSize);
            return 0;
        }

        // Wait Queue에서 Pop
        List<String> userIds = queueRepository.popFromWaitQueue(concertId, availableSlots);

        if (userIds.isEmpty()) {
            log.debug("No users waiting: concertId={}", concertId);
            return 0;
        }

        // Active Queue에 추가 (READY 상태, TTL 5분)
        Instant expiration = domainService.calculateReadyExpiration();
        int movedCount = 0;

        for (String userId : userIds) {
            try {
                String token = domainService.generateToken();
                queueRepository.addToActiveQueue(concertId, userId, token, expiration);
                movedCount++;
                log.info("User moved to active queue: concertId={}, userId={}, token={}",
                        concertId, userId, token);
            } catch (Exception e) {
                log.error("Failed to move user to active queue: concertId={}, userId={}",
                        concertId, userId, e);
            }
        }

        log.info("Moved users to active queue: concertId={}, moved={}, available={}",
                concertId, movedCount, availableSlots);

        return movedCount;
    }

    @Override
    public int moveAllConcerts() {
        // TODO: 실제 구현 시 활성화된 콘서트 목록을 조회하여 처리
        // 현재는 단순 구현
        log.debug("Moving all concerts - not implemented yet");
        return 0;
    }

    @Override
    public long cleanupExpired(String concertId) {
        log.debug("Cleaning up expired tokens: concertId={}", concertId);

        long removedCount = queueRepository.removeExpiredTokens(concertId);

        if (removedCount > 0) {
            log.info("Removed expired tokens: concertId={}, count={}", concertId, removedCount);
        }

        return removedCount;
    }

    @Override
    public long cleanupAllConcerts() {
        // TODO: 실제 구현 시 활성화된 콘서트 목록을 조회하여 처리
        // 현재는 단순 구현
        log.debug("Cleaning up all concerts - not implemented yet");
        return 0;
    }
}
