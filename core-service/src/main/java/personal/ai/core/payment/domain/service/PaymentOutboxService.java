package personal.ai.core.payment.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import personal.ai.core.payment.application.port.out.PaymentEventPublisher;
import personal.ai.core.payment.application.port.out.PaymentOutboxRepository;
import personal.ai.core.payment.domain.model.PaymentOutboxEvent;

import java.util.List;

/**
 * Payment Outbox Service
 * 대기 중인 결제 이벤트를 발행 처리하는 도메인 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxService {

    private static final int MAX_RETRY_COUNT = 3;
    private static final String TOPIC_PAYMENT_COMPLETED = "booking.payment.completed";

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public int publishPendingEvents() {
        List<PaymentOutboxEvent> pendingEvents = paymentOutboxRepository.findPendingEvents();
        int publishedCount = 0;

        for (PaymentOutboxEvent event : pendingEvents) {
            try {
                String topic = mapEventTypeToTopic(event.eventType());

                // Key: Aggregate ID (paymentId) to ensure ordering
                String key = String.valueOf(event.aggregateId());

                log.debug("Publishing payment event: id={}, type={}, topic={}",
                        event.id(), event.eventType(), topic);

                // Publish Raw Payload directly
                paymentEventPublisher.publishRaw(topic, key, event.payload());

                // Update Status (Immutable)
                PaymentOutboxEvent publishedEvent = event.markAsPublished();
                paymentOutboxRepository.save(publishedEvent);
                publishedCount++;

                log.info("Payment event published successfully: id={}, type={}",
                        event.id(), event.eventType());

            } catch (Exception e) {
                log.error("Failed to publish payment event: id={}", event.id(), e);

                // Retry Logic (Immutable)
                PaymentOutboxEvent retriedEvent = event.incrementRetryCount();

                if (retriedEvent.retryCount() >= MAX_RETRY_COUNT) {
                    retriedEvent = retriedEvent.markAsFailed();
                    log.error("Payment event marked as FAILED after {} retries: id={}",
                            MAX_RETRY_COUNT, event.id());
                }

                paymentOutboxRepository.save(retriedEvent);
            }
        }

        if (publishedCount > 0) {
            log.info("Published {} payment outbox events", publishedCount);
        }

        return publishedCount;
    }

    private String mapEventTypeToTopic(String eventType) {
        return switch (eventType) {
            case "PAYMENT_COMPLETED" -> TOPIC_PAYMENT_COMPLETED;
            default -> throw new IllegalArgumentException("Unknown payment event type: " + eventType);
        };
    }
}
