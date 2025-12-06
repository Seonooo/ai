package personal.ai.core.payment.application.port.out;

import personal.ai.core.payment.domain.model.PaymentOutboxEvent;

import java.util.List;

/**
 * Payment Outbox Repository Port
 */
public interface PaymentOutboxRepository {

    /**
     * Outbox Event 저장
     */
    PaymentOutboxEvent save(PaymentOutboxEvent event);

    /**
     * PENDING 상태의 이벤트 조회
     */
    List<PaymentOutboxEvent> findPendingEvents();
}
