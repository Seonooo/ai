package personal.ai.core.payment.domain.exception;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

/**
 * Payment Already Completed Exception
 */
public class PaymentAlreadyCompletedException extends BusinessException {
    public PaymentAlreadyCompletedException(Long paymentId) {
        super(ErrorCode.CONFLICT, String.format("Payment already completed: %d", paymentId));
    }
}
