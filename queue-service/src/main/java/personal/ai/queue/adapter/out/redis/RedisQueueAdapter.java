package personal.ai.queue.adapter.out.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import personal.ai.queue.application.port.out.QueueRepository;
import personal.ai.queue.domain.model.QueueStatus;
import personal.ai.queue.domain.model.QueueToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Redis Queue Adapter (Facade Pattern)
 * QueueRepository 인터페이스 구현체로, 실제 작업은 전문화된 어댑터들에게 위임합니다.
 *
 * 책임 분리:
 * - RedisWaitQueueAdapter: Wait Queue 관련 작업
 * - RedisActiveQueueAdapter: Active Queue 관련 작업
 * - RedisTokenConverter: 데이터 변환
 * - RedisLuaScriptExecutor: Lua 스크립트 실행
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisQueueAdapter implements QueueRepository {

    private final RedisWaitQueueAdapter waitQueueAdapter;
    private final RedisActiveQueueAdapter activeQueueAdapter;

    // ========== Wait Queue 관련 메서드 ==========

    @Override
    public Long addToWaitQueue(String concertId, String userId) {
        return waitQueueAdapter.addToWaitQueue(concertId, userId);
    }

    @Override
    public Long getWaitQueuePosition(String concertId, String userId) {
        return waitQueueAdapter.getWaitQueuePosition(concertId, userId);
    }

    @Override
    public Long getWaitQueueSize(String concertId) {
        return waitQueueAdapter.getWaitQueueSize(concertId);
    }

    @Override
    public List<String> popFromWaitQueue(String concertId, int count) {
        return waitQueueAdapter.popFromWaitQueue(concertId, count);
    }

    @Override
    public void removeFromWaitQueue(String concertId, String userId) {
        waitQueueAdapter.removeFromWaitQueue(concertId, userId);
    }

    // ========== Active Queue 관련 메서드 ==========

    @Override
    public void addToActiveQueue(String concertId, String userId, String token, Instant expiredAt) {
        activeQueueAdapter.addToActiveQueue(concertId, userId, token, expiredAt);
    }

    @Override
    public Optional<QueueToken> getActiveToken(String concertId, String userId) {
        return activeQueueAdapter.getActiveToken(concertId, userId);
    }

    @Override
    public void updateTokenExpiration(String concertId, String userId, Instant expiredAt) {
        activeQueueAdapter.updateTokenExpiration(concertId, userId, expiredAt);
    }

    @Override
    public void updateTokenStatus(String concertId, String userId, QueueStatus status) {
        activeQueueAdapter.updateTokenStatus(concertId, userId, status);
    }

    @Override
    public Integer incrementExtendCount(String concertId, String userId) {
        return activeQueueAdapter.incrementExtendCount(concertId, userId);
    }

    @Override
    public Long getActiveQueueSize(String concertId) {
        return activeQueueAdapter.getActiveQueueSize(concertId);
    }

    @Override
    public Long removeExpiredTokens(String concertId) {
        return activeQueueAdapter.removeExpiredTokens(concertId);
    }

    @Override
    public void removeFromActiveQueue(String concertId, String userId) {
        activeQueueAdapter.removeFromActiveQueue(concertId, userId);
    }

    // ========== 배치 작업 메서드 ==========

    @Override
    public List<String> moveToActiveQueueAtomic(String concertId, int count, Instant expiredAt) {
        return activeQueueAdapter.moveToActiveQueueAtomic(concertId, count, expiredAt);
    }

    @Override
    public boolean activateTokenAtomic(String concertId, String userId, Instant newExpiredAt) {
        return activeQueueAdapter.activateTokenAtomic(concertId, userId, newExpiredAt);
    }

    // ========== 조회 메서드 ==========

    /**
     * 활성 상태인 콘서트 ID 목록을 조회합니다.
     * Wait Queue 또는 Active Queue에 데이터가 있는 콘서트들을 반환합니다.
     *
     * @return 활성 콘서트 ID 리스트
     */
    @Override
    public List<String> getActiveConcertIds() {
        var waitIds = waitQueueAdapter.getWaitQueueConcertIds();
        var activeIds = activeQueueAdapter.getActiveQueueConcertIds();

        return Stream.concat(waitIds.stream(), activeIds.stream())
                .distinct()
                .toList();
    }
}