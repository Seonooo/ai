package personal.ai.queue.adapter.in.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import personal.ai.queue.application.port.in.RemoveFromQueueUseCase;

/**
 * Payment Event Kafka Consumer (Inbound Adapter)
 * 결제 완료 이벤트를 구독하여 대기열에서 유저 제거
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final RemoveFromQueueUseCase removeFromQueueUseCase;
    private final ObjectMapper objectMapper;

    /**
     * 결제 완료 이벤트 처리
     * Topic: booking.payment.completed
     *
     * architecture.md:
     * - Queue Service는 이 이벤트를 구독하여 해당 유저를 대기열에서 즉시 삭제
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

            // Active Queue에서 유저 제거 (UseCase를 통해)
            RemoveFromQueueUseCase.RemoveFromQueueCommand command =
                    new RemoveFromQueueUseCase.RemoveFromQueueCommand(
                            event.concertId(),
                            event.userId()
                    );
            removeFromQueueUseCase.removeFromQueue(command);

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
