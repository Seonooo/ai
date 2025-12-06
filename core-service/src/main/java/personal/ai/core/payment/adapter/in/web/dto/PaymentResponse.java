package personal.ai.core.payment.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import personal.ai.core.payment.domain.model.Payment;
import personal.ai.core.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Response DTO
 */
public record PaymentResponse(
        @JsonProperty("paymentId")
        Long paymentId,

        @JsonProperty("reservationId")
        Long reservationId,

        @JsonProperty("userId")
        Long userId,

        @JsonProperty("amount")
        BigDecimal amount,

        @JsonProperty("status")
        PaymentStatus status,

        @JsonProperty("paymentMethod")
        String paymentMethod,

        @JsonProperty("paidAt")
        LocalDateTime paidAt,

        @JsonProperty("createdAt")
        LocalDateTime createdAt
) {
    /**
     * 도메인 모델로부터 DTO 생성
     */
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.reservationId(),
                payment.userId(),
                payment.amount(),
                payment.status(),
                payment.paymentMethod(),
                payment.paidAt(),
                payment.createdAt()
        );
    }
}
