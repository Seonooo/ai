package personal.ai.core.payment.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import personal.ai.core.payment.domain.model.Payment;
import personal.ai.core.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment JPA Entity
 */
@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payment_reservation_id", columnList = "reservation_id"),
                @Index(name = "idx_payment_user_id", columnList = "user_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 도메인 모델로부터 엔티티 생성
     */
    public static PaymentEntity from(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.id = payment.id();
        entity.reservationId = payment.reservationId();
        entity.userId = payment.userId();
        entity.amount = payment.amount();
        entity.status = payment.status();
        entity.paymentMethod = payment.paymentMethod();
        entity.paidAt = payment.paidAt();
        entity.createdAt = payment.createdAt() != null ? payment.createdAt() : LocalDateTime.now();
        return entity;
    }

    /**
     * 엔티티를 도메인 모델로 변환
     */
    public Payment toDomain() {
        return new Payment(
                id,
                reservationId,
                userId,
                amount,
                status,
                paymentMethod,
                paidAt,
                createdAt
        );
    }
}
