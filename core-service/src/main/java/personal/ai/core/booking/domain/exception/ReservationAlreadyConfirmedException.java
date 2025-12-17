package personal.ai.core.booking.domain.exception;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

/**
 * 이미 확정된 예약에 대한 중복 결제 시도 예외
 * HTTP 409 CONFLICT 반환
 */
public class ReservationAlreadyConfirmedException extends BusinessException {
    public ReservationAlreadyConfirmedException(Long reservationId) {
        super(ErrorCode.CONFLICT,
                String.format("이미 확정된 예약입니다. 중복 결제할 수 없습니다: reservationId=%d", reservationId));
    }
}
