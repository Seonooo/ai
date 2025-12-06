package personal.ai.core.payment.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import personal.ai.core.payment.domain.model.PaymentOutboxEvent;

import java.time.LocalDateTime;

/**
 * Payment Outbox Event JPA Entity
 */
@Entity
@Table(name = "payment_outbox_events",
        indexes = {
                @Index(name = "idx_payment_outbox_status", columnList = "status"),
                @Index(name = "idx_payment_outbox_created_at", columnList = "created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOutboxEvent.OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * 정적 팩토리 메서드
     */
    public static PaymentOutboxEventEntity create(
            String aggregateType,
            Long aggregateId,
            String eventType,
            String payload) {
        PaymentOutboxEventEntity entity = new PaymentOutboxEventEntity();
        entity.aggregateType = aggregateType;
        entity.aggregateId = aggregateId;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.status = PaymentOutboxEvent.OutboxStatus.PENDING;
        entity.retryCount = 0;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }

    /**
     * 도메인 모델로부터 엔티티 생성
     */
    public static PaymentOutboxEventEntity from(PaymentOutboxEvent event) {
        PaymentOutboxEventEntity entity = new PaymentOutboxEventEntity();
        entity.id = event.id();
        entity.aggregateType = event.aggregateType();
        entity.aggregateId = event.aggregateId();
        entity.eventType = event.eventType();
        entity.payload = event.payload();
        entity.status = event.status();
        entity.retryCount = event.retryCount();
        entity.createdAt = event.createdAt();
        entity.publishedAt = event.publishedAt();
        return entity;
    }

    /**
     * 엔티티를 도메인 모델로 변환
     */
    public PaymentOutboxEvent toDomain() {
        return new PaymentOutboxEvent(
                id,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                status,
                retryCount,
                createdAt,
                publishedAt
        );
    }
}
