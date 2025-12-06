package personal.ai.core.payment.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import personal.ai.core.payment.application.port.out.PaymentOutboxRepository;
import personal.ai.core.payment.domain.model.PaymentOutboxEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment Outbox Persistence Adapter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxPersistenceAdapter implements PaymentOutboxRepository {

    private final JpaPaymentOutboxRepository jpaPaymentOutboxRepository;

    @Override
    public PaymentOutboxEvent save(PaymentOutboxEvent event) {
        log.debug("Saving payment outbox event: eventType={}, aggregateId={}",
                event.eventType(), event.aggregateId());

        PaymentOutboxEventEntity entity = PaymentOutboxEventEntity.from(event);
        PaymentOutboxEventEntity saved = jpaPaymentOutboxRepository.save(entity);

        return saved.toDomain();
    }

    @Override
    public List<PaymentOutboxEvent> findPendingEvents() {
        return jpaPaymentOutboxRepository
                .findByStatusOrderByCreatedAtAsc(PaymentOutboxEvent.OutboxStatus.PENDING)
                .stream()
                .map(PaymentOutboxEventEntity::toDomain)
                .collect(Collectors.toList());
    }
}
