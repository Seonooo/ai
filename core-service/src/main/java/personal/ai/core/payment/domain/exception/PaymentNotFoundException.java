package personal.ai.core.payment.domain.exception;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

/**
 * Payment Not Found Exception
 */
public class PaymentNotFoundException extends BusinessException {
    public PaymentNotFoundException(Long paymentId) {
        super(ErrorCode.NOT_FOUND, String.format("Payment not found: %d", paymentId));
    }
}
