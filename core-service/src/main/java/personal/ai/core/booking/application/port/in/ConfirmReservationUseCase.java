package personal.ai.core.booking.application.port.in;

import personal.ai.core.booking.domain.model.Reservation;

/**
 * Confirm Reservation Use Case
 * 예약 확정 (결제 완료 후)
 */
public interface ConfirmReservationUseCase {

    /**
     * 예약 확정 및 좌석 점유
     *
     * @param command 확정 명령
     * @return 확정된 예약
     */
    Reservation confirmReservation(ConfirmReservationCommand command);

    /**
     * 예약 확정 명령
     */
    record ConfirmReservationCommand(
            Long reservationId,
            Long userId,
            Long paymentId
    ) {
    }
}
