package personal.ai.core.payment.application.port.out;

import personal.ai.core.payment.domain.model.Payment;

/**
 * Payment Event Publisher Port
 * 결제 이벤트 발행
 */
public interface PaymentEventPublisher {

    /**
     * 결제 완료 이벤트 발행
     */
    void publishPaymentCompleted(Payment payment, String concertId);

    /**
     * Raw Event 발행 (Outbox Pattern용)
     * 이미 직렬화된 JSON Payload를 그대로 발행
     *
     * @param topic   발행할 Kafka 토픽
     * @param key     메시지 키 (순서 보장용, e.g. paymentId)
     * @param payload 메시지 본문 (JSON String)
     */
    void publishRaw(String topic, String key, String payload);
}
