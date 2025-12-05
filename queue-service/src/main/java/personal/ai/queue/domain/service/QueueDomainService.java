package personal.ai.queue.domain.service;

import personal.ai.queue.domain.exception.QueueExtensionLimitExceededException;
import personal.ai.queue.domain.exception.QueueTokenInvalidException;
import personal.ai.queue.domain.model.QueueConfig;
import personal.ai.queue.domain.model.QueueToken;

import java.time.Instant;
import java.util.UUID;

/**
 * Queue Domain Service
 * 순수 비즈니스 로직만 포함 (외부 의존성 없음)
 */
public class QueueDomainService {

    private final QueueConfig config;

    public QueueDomainService(QueueConfig config) {
        this.config = config;
    }

    /**
     * 새로운 토큰 생성 (UUID)
     */
    public String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Ready 상태의 만료 시간 계산 (진입 대기)
     * 현재 시간 + 5분
     */
    public Instant calculateReadyExpiration() {
        return Instant.now().plusSeconds(config.tokenTtlSeconds());
    }

    /**
     * Active 상태의 만료 시간 계산 (활동 보장)
     * 현재 시간 + 10분
     */
    public Instant calculateActiveExpiration() {
        return Instant.now().plusSeconds(config.activatedTtlSeconds());
    }

    /**
     * 토큰 연장 검증
     * @throws QueueExtensionLimitExceededException 연장 한도 초과
     * @throws QueueTokenInvalidException 활성 상태가 아닌 토큰
     */
    public void validateExtension(QueueToken token) {
        if (!token.canExtend()) {
            throw new QueueExtensionLimitExceededException(token.concertId(), token.userId());
        }

        if (!token.isActive()) {
            throw new QueueTokenInvalidException(token.concertId(), token.userId());
        }
    }

    /**
     * 활성 큐 여유 공간 확인
     */
    public boolean hasActiveCapacity(long currentActiveCount) {
        return currentActiveCount < config.activeMaxSize();
    }

    /**
     * 한 번에 전환 가능한 최대 인원 계산
     */
    public int calculateBatchSize(long currentActiveCount) {
        long available = config.activeMaxSize() - currentActiveCount;
        return (int) Math.max(0, available);
    }
}
