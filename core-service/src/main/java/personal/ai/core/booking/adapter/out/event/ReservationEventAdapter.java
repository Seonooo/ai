package personal.ai.core.booking.adapter.out.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import personal.ai.common.exception.OutboxEventException;
import personal.ai.core.booking.adapter.out.persistence.JpaOutboxEventRepository;
import personal.ai.core.booking.adapter.out.persistence.OutboxEventEntity;
import personal.ai.core.booking.adapter.out.persistence.OutboxEventFactory;
import personal.ai.core.booking.application.port.out.ReservationEventPort;
import personal.ai.core.booking.domain.model.Reservation;

/**
 * Reservation Event Adapter
 * Outbox 패턴을 사용한 예약 이벤트 발행 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventAdapter implements ReservationEventPort {

    private final JpaOutboxEventRepository jpaOutboxEventRepository;
    private final OutboxEventFactory outboxEventFactory;

    /**
     * 예약 상태 변경 이벤트 발행 (Outbox 패턴)
     * 
     * Transactional MANDATORY:
     * - Outbox 이벤트는 반드시 비즈니스 로직과 같은 트랜잭션에서 저장되어야 함
     * - 호출자가 트랜잭션을 시작하지 않으면 즉시 예외 발생 (Fail-Fast)
     * - 데이터 정합성 보장을 아키텍처 레벨에서 강제
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishReservationEvent(Reservation reservation) {
        try {
            OutboxEventEntity outboxEvent = switch (reservation.status()) {
                case PENDING -> outboxEventFactory.createReservationCreatedEvent(reservation);
                case CONFIRMED -> outboxEventFactory.createReservationConfirmedEvent(reservation);
                case CANCELLED -> outboxEventFactory.createReservationCancelledEvent(reservation);
                case EXPIRED -> outboxEventFactory.createReservationExpiredEvent(reservation);
            };

            jpaOutboxEventRepository.save(outboxEvent);
            log.debug("Reservation event published: reservationId={}, status={}",
                    reservation.id(), reservation.status());

        } catch (Exception e) {
            log.error("Failed to publish reservation event: reservationId={}", reservation.id(), e);
            throw OutboxEventException.saveFailed(reservation.id(), e);
        }
    }
}
