package personal.ai.core.payment.adapter.out.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import personal.ai.core.payment.application.port.out.PaymentEventPublisher;
import personal.ai.core.payment.domain.model.Payment;

import java.time.Instant;
import java.util.UUID;

/**
 * Payment Kafka Publisher (Adapter Layer)
 * Kafka를 통한 결제 이벤트 발행 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentKafkaPublisher implements PaymentEventPublisher {

    private static final String TOPIC_PAYMENT_COMPLETED = "booking.payment.completed";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.payment-completed:" + TOPIC_PAYMENT_COMPLETED + "}")
    private String paymentCompletedTopic;

    @Override
    public void publishPaymentCompleted(Payment payment, String concertId) {
        log.info("Publishing payment completed event: paymentId={}, reservationId={}",
                payment.id(), payment.reservationId());

        // 이벤트 생성
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID().toString(),
                concertId,
                payment.userId().toString(),
                payment.reservationId().toString(),
                payment.id().toString(),
                payment.amount().longValue(),
                Instant.now()
        );

        try {
            // JSON 직렬화
            String payload = objectMapper.writeValueAsString(event);

            // Kafka 발행 (동기 방식으로 전송하여 확인)
            kafkaTemplate.send(paymentCompletedTopic, payment.userId().toString(), payload)
                    .join();

            log.info("Payment completed event published successfully: {}",
                    event.toLogString());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment completed event: paymentId={}",
                    payment.id(), e);
            throw new RuntimeException("Failed to publish payment event", e);
        } catch (Exception e) {
            log.error("Failed to publish payment completed event: paymentId={}",
                    payment.id(), e);
            throw new RuntimeException("Failed to publish payment event", e);
        }
    }

    @Override
    public void publishRaw(String topic, String key, String payload) {
        log.debug("Publishing raw payment event: topic={}, key={}", topic, key);

        try {
            // String payload를 그대로 전송 (StringSerializer 사용)
            // 동기 방식(join)으로 변환하여 DB 상태 업데이트와 정합성 보장
            kafkaTemplate.send(topic, key, payload).join();
            log.debug("Raw payment event published: topic={}, key={}", topic, key);
        } catch (Exception e) {
            log.error("Failed to publish raw payment event: topic={}, key={}", topic, key, e);
            throw new RuntimeException("Kafka publish failed", e);
        }
    }
}
