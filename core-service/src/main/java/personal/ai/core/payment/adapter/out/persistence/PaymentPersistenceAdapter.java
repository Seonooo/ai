package personal.ai.core.payment.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import personal.ai.core.payment.application.port.out.PaymentRepository;
import personal.ai.core.payment.domain.model.Payment;

import java.util.Optional;

/**
 * Payment Persistence Adapter
 * JPA를 사용한 결제 저장소 구현
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentRepository {

    private final JpaPaymentRepository jpaPaymentRepository;

    @Override
    public Payment save(Payment payment) {
        log.debug("Saving payment: reservationId={}", payment.reservationId());
        PaymentEntity entity = PaymentEntity.from(payment);
        PaymentEntity saved = jpaPaymentRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Payment> findById(Long id) {
        log.debug("Finding payment by id: {}", id);
        return jpaPaymentRepository.findById(id)
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByReservationId(Long reservationId) {
        log.debug("Finding payment by reservationId: {}", reservationId);
        return jpaPaymentRepository.findByReservationId(reservationId)
                .map(PaymentEntity::toDomain);
    }
}
