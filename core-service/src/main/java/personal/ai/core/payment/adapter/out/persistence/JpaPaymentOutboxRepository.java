package personal.ai.core.payment.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import personal.ai.core.payment.domain.model.PaymentOutboxEvent;

import java.util.List;

/**
 * JPA Payment Outbox Repository
 */
public interface JpaPaymentOutboxRepository extends JpaRepository<PaymentOutboxEventEntity, Long> {

    /**
     * PENDING 상태의 이벤트 조회
     */
    List<PaymentOutboxEventEntity> findByStatusOrderByCreatedAtAsc(PaymentOutboxEvent.OutboxStatus status);
}
