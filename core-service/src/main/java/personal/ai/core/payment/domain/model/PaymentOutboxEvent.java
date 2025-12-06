package personal.ai.core.payment.domain.model;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

import java.time.LocalDateTime;

/**
 * Payment Outbox Event Domain Model
 * 결제 이벤트 발행을 위한 도메인 객체 (불변)
 */
public record PaymentOutboxEvent(
        Long id,
        String aggregateType,
        Long aggregateId,
        String eventType,
        String payload,
        OutboxStatus status,
        int retryCount,
        LocalDateTime createdAt,
        LocalDateTime publishedAt) {

    public PaymentOutboxEvent {
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Aggregate Type cannot be empty");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Event Type cannot be empty");
        }
        if (payload == null || payload.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Payload cannot be empty");
        }
        if (status == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Status cannot be null");
        }
    }

    /**
     * 발행 성공 처리
     */
    public PaymentOutboxEvent markAsPublished() {
        return new PaymentOutboxEvent(id, aggregateType, aggregateId, eventType, payload,
                OutboxStatus.PUBLISHED, retryCount, createdAt, LocalDateTime.now());
    }

    /**
     * 재시도 횟수 증가
     */
    public PaymentOutboxEvent incrementRetryCount() {
        return new PaymentOutboxEvent(id, aggregateType, aggregateId, eventType, payload,
                status, retryCount + 1, createdAt, publishedAt);
    }

    /**
     * 발행 실패 처리 (최종 실패)
     */
    public PaymentOutboxEvent markAsFailed() {
        return new PaymentOutboxEvent(id, aggregateType, aggregateId, eventType, payload,
                OutboxStatus.FAILED, retryCount, createdAt, publishedAt);
    }

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }
}
