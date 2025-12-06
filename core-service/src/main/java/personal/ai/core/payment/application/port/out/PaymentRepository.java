package personal.ai.core.payment.application.port.out;

import personal.ai.core.payment.domain.model.Payment;

import java.util.Optional;

/**
 * Payment Repository Port
 */
public interface PaymentRepository {

    /**
     * 결제 저장
     */
    Payment save(Payment payment);

    /**
     * 결제 조회 (ID)
     */
    Optional<Payment> findById(Long id);

    /**
     * 예약 ID로 결제 조회
     */
    Optional<Payment> findByReservationId(Long reservationId);
}
