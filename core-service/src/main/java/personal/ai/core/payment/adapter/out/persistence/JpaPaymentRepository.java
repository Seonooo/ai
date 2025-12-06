package personal.ai.core.payment.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * JPA Payment Repository
 */
public interface JpaPaymentRepository extends JpaRepository<PaymentEntity, Long> {

    /**
     * 예약 ID로 결제 조회
     */
    Optional<PaymentEntity> findByReservationId(Long reservationId);
}
