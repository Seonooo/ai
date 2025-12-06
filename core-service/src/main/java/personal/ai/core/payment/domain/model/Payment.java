package personal.ai.core.payment.domain.model;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Domain Model
 * 결제 도메인 모델 (불변)
 */
public record Payment(
        Long id,
        Long reservationId,
        Long userId,
        BigDecimal amount,
        PaymentStatus status,
        String paymentMethod,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public Payment {
        if (reservationId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Reservation ID cannot be null");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "User ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Payment amount must be positive");
        }
        if (status == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Payment status cannot be null");
        }
        if (createdAt == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Creation time cannot be null");
        }
    }

    /**
     * 결제 생성 (정적 팩토리 메서드)
     *
     * @param reservationId 예약 ID
     * @param userId        사용자 ID
     * @param amount        결제 금액
     * @param paymentMethod 결제 수단
     * @return 새로운 결제 (PENDING 상태)
     */
    public static Payment create(Long reservationId, Long userId, BigDecimal amount, String paymentMethod) {
        return new Payment(
                null,
                reservationId,
                userId,
                amount,
                PaymentStatus.PENDING,
                paymentMethod,
                null,
                LocalDateTime.now()
        );
    }

    /**
     * 결제 완료 (PENDING -> COMPLETED)
     */
    public Payment complete() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("Cannot complete payment in %s status. Payment ID: %d", status, id)
            );
        }
        return new Payment(id, reservationId, userId, amount,
                PaymentStatus.COMPLETED, paymentMethod, LocalDateTime.now(), createdAt);
    }

    /**
     * 결제 실패 (PENDING -> FAILED)
     */
    public Payment fail() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("Cannot fail payment in %s status. Payment ID: %d", status, id)
            );
        }
        return new Payment(id, reservationId, userId, amount,
                PaymentStatus.FAILED, paymentMethod, null, createdAt);
    }

    /**
     * 결제 취소 (COMPLETED -> CANCELLED)
     */
    public Payment cancel() {
        if (status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    String.format("Cannot cancel payment in %s status. Payment ID: %d", status, id)
            );
        }
        return new Payment(id, reservationId, userId, amount,
                PaymentStatus.CANCELLED, paymentMethod, paidAt, createdAt);
    }

    /**
     * 결제 완료 여부 확인
     */
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    /**
     * 결제 대기 여부 확인
     */
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
}
