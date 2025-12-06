package personal.ai.core.payment.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import personal.ai.core.payment.domain.service.PaymentOutboxService;

/**
 * Payment Outbox Event Scheduler
 * 주기적으로 PENDING 상태의 결제 이벤트를 Kafka로 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler {

    private final PaymentOutboxService paymentOutboxService;

    /**
     * 5초마다 PENDING 이벤트 발행
     */
    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        try {
            int publishedCount = paymentOutboxService.publishPendingEvents();

            if (publishedCount > 0) {
                log.debug("Payment outbox scheduler published {} events", publishedCount);
            }
        } catch (Exception e) {
            log.error("Error in payment outbox scheduler", e);
        }
    }
}
