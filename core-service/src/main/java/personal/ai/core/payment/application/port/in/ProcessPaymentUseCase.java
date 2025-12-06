package personal.ai.core.payment.application.port.in;

import personal.ai.core.payment.domain.model.Payment;

import java.math.BigDecimal;

/**
 * Process Payment Use Case
 * 결제 처리
 */
public interface ProcessPaymentUseCase {

    /**
     * 결제 처리
     *
     * @param command 결제 명령
     * @return 완료된 결제
     */
    Payment processPayment(ProcessPaymentCommand command);

    /**
     * 결제 처리 명령
     */
    record ProcessPaymentCommand(
            Long reservationId,
            Long userId,
            BigDecimal amount,
            String paymentMethod,
            String concertId
    ) {
    }
}
