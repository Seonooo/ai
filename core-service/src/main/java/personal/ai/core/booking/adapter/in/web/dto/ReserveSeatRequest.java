package personal.ai.core.booking.adapter.in.web.dto;

import personal.ai.core.booking.application.port.in.ReserveSeatCommand;

/**
 * Reserve Seat Request DTO
 * 좌석 예약 요청
 */
public record ReserveSeatRequest(
        Long seatId,
        Long scheduleId
) {
    public ReserveSeatCommand toCommand(Long userId, String queueToken) {
        return new ReserveSeatCommand(userId, seatId, scheduleId, queueToken);
    }
}
