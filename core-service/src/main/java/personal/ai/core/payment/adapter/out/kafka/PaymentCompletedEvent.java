package personal.ai.core.payment.adapter.out.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Payment Completed Event (Kafka Message)
 * 결제 완료 이벤트
 */
public record PaymentCompletedEvent(
        @JsonProperty("eventId")
        String eventId,

        @JsonProperty("concertId")
        String concertId,

        @JsonProperty("userId")
        String userId,

        @JsonProperty("reservationId")
        String reservationId,

        @JsonProperty("paymentId")
        String paymentId,

        @JsonProperty("amount")
        Long amount,

        @JsonProperty("timestamp")
        Instant timestamp
) {
    /**
     * JSON 문자열로 변환 (로깅용)
     */
    public String toLogString() {
        return "PaymentCompletedEvent{" +
                "eventId='" + eventId + '\'' +
                ", concertId='" + concertId + '\'' +
                ", userId='" + userId + '\'' +
                ", reservationId='" + reservationId + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", amount=" + amount +
                '}';
    }
}
