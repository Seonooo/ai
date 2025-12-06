package personal.ai.core.payment.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Process Payment Request DTO
 */
public record ProcessPaymentRequest(
        @JsonProperty("reservationId")
        Long reservationId,

        @JsonProperty("userId")
        Long userId,

        @JsonProperty("amount")
        BigDecimal amount,

        @JsonProperty("paymentMethod")
        String paymentMethod,

        @JsonProperty("concertId")
        String concertId
) {
}
