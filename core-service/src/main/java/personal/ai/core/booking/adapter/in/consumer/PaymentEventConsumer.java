package personal.ai.core.booking.adapter.in.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import personal.ai.core.booking.application.port.in.ConfirmReservationUseCase;
import personal.ai.core.payment.adapter.out.kafka.PaymentCompletedEvent;

/**
 * Payment Event Kafka Consumer (Inbound Adapter)
 * 결제 완료 이벤트를 구독하여 예약 확정 및 좌석 점유 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ConfirmReservationUseCase confirmReservationUseCase;
    private final ObjectMapper objectMapper;

    /**
     * 결제 완료 이벤트 처리
     * Topic: booking.payment.completed
     *
     * 처리 내용:
     * 1. 예약 상태를 PENDING -> CONFIRMED로 변경
     * 2. 좌석 상태를 RESERVED -> OCCUPIED로 변경
     * 3. Queue Service는 별도로 이 이벤트를 구독하여 Active Queue에서 유저 제거
     */
    @KafkaListener(
            topics = "${kafka.topic.payment-completed:booking.payment.completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Received payment completed event: topic={}, partition={}, offset={}",
                    topic, partition, offset);

            // JSON 역직렬화
            PaymentCompletedEvent event = objectMapper.readValue(
                    message,
                    PaymentCompletedEvent.class
            );

            log.info("Processing payment completed event: {}", event.toLogString());

            // 예약 확정 및 좌석 점유
            ConfirmReservationUseCase.ConfirmReservationCommand command =
                    new ConfirmReservationUseCase.ConfirmReservationCommand(
                            Long.parseLong(event.reservationId()),
                            Long.parseLong(event.userId()),
                            Long.parseLong(event.paymentId())
                    );

            confirmReservationUseCase.confirmReservation(command);

            log.info("Reservation confirmed successfully: reservationId={}, userId={}",
                    event.reservationId(), event.userId());

            // Manual Ack (처리 완료 확인)
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Failed to process payment completed event: topic={}, partition={}, offset={}",
                    topic, partition, offset, e);

            // 에러 발생 시 Ack하지 않음 -> 재처리
            // 또는 DLQ(Dead Letter Queue)로 전송
            throw new RuntimeException("Payment event processing failed", e);
        }
    }
}
