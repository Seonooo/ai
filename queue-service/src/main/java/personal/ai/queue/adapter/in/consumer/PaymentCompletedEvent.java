package personal.ai.queue.adapter.in.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * 결제 완료 이벤트 (Kafka Message)
 * Booking Service에서 발행
 */
public record PaymentCompletedEvent(
        @JsonProperty("eventId")
        String eventId,

        @JsonProperty("concertId")
        String concertId,

        @JsonProperty("userId")
        String userId,

        @JsonProperty("bookingId")
        String bookingId,

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
                ", bookingId='" + bookingId + '\'' +
                '}';
    }
}
