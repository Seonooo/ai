package personal.ai.core.payment.domain.model;

/**
 * Payment Status Enum
 * 결제 상태
 */
public enum PaymentStatus {
    /**
     * 결제 대기
     */
    PENDING,

    /**
     * 결제 완료
     */
    COMPLETED,

    /**
     * 결제 실패
     */
    FAILED,

    /**
     * 결제 취소
     */
    CANCELLED
}
