package personal.ai.core.payment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import personal.ai.core.booking.application.port.out.ReservationRepository;
import personal.ai.core.booking.application.port.out.SeatLockRepository;
import personal.ai.core.booking.domain.exception.ReservationNotFoundException;
import personal.ai.core.booking.domain.model.Reservation;
import personal.ai.core.payment.adapter.out.kafka.PaymentCompletedEvent;
import personal.ai.core.payment.adapter.out.persistence.PaymentOutboxEventEntity;
import personal.ai.core.payment.application.port.in.ProcessPaymentUseCase;
import personal.ai.core.payment.application.port.out.PaymentOutboxRepository;
import personal.ai.core.payment.application.port.out.PaymentRepository;
import personal.ai.core.payment.domain.exception.PaymentAlreadyCompletedException;
import personal.ai.core.payment.domain.model.Payment;
import personal.ai.core.payment.domain.service.PaymentMockService;

import java.time.Instant;
import java.util.UUID;

import static personal.ai.common.exception.ErrorCode.FORBIDDEN;
import static personal.ai.common.exception.ErrorCode.INVALID_INPUT;

/**
 * Payment Application Service
 * 결제 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService implements ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentMockService paymentMockService;
    private final SeatLockRepository seatLockRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Payment processPayment(ProcessPaymentCommand command) {
        log.info("Processing payment: reservationId={}, userId={}, amount={}",
                command.reservationId(), command.userId(), command.amount());

        // 1. 예약 확인 및 검증 (읽기 전용)
        Reservation reservation = validateReservationForPayment(command);

        // 2. 결제 생성 (PENDING 상태로 먼저 DB 저장) - Tx1
        Payment pendingPayment = createPendingPayment(command);

        // 3. Mock 결제 처리 (트랜잭션 범위 밖 - No Tx)
        boolean paymentSuccess = processMockPayment(
                command.userId(),
                command.reservationId(),
                command.amount().longValue()
        );

        // 4. 결제 결과에 따른 처리 - Tx2
        if (paymentSuccess) {
            return handlePaymentSuccess(pendingPayment, command.concertId());
        } else {
            handlePaymentFailure(pendingPayment, reservation, command.userId());
            throw new personal.ai.common.exception.BusinessException(
                    personal.ai.common.exception.ErrorCode.PAYMENT_FAILED,
                    "결제가 거절되었습니다.");
        }
    }

    /**
     * 예약 검증 (읽기 전용 - 트랜잭션 없음)
     */
    private Reservation validateReservationForPayment(ProcessPaymentCommand command) {
        // 1. 예약 확인
        Reservation reservation = reservationRepository.findById(command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));

        // 2. 예약 소유권 확인
        if (!reservation.userId().equals(command.userId())) {
            throw new personal.ai.common.exception.BusinessException(
                    FORBIDDEN,
                    "해당 예약에 대한 결제 권한이 없습니다.");
        }

        // 3. 예약 상태 확인 (PENDING만 결제 가능)
        if (!reservation.isPending()) {
            throw new personal.ai.common.exception.BusinessException(
                    INVALID_INPUT,
                    String.format("예약 상태가 %s이므로 결제할 수 없습니다.", reservation.status()));
        }

        // 4. 예약 만료 확인
        if (reservation.isExpired()) {
            throw new personal.ai.core.booking.domain.exception.ReservationExpiredException(
                    command.reservationId());
        }

        return reservation;
    }

    /**
     * PENDING 상태 결제 생성 (Tx1 - 짧은 트랜잭션)
     * 동시성 제어: DB Unique Constraint로 중복 결제 방지
     */
    @Transactional
    protected Payment createPendingPayment(ProcessPaymentCommand command) {
        // 기존 결제 확인 (1차 방어)
        paymentRepository.findByReservationId(command.reservationId())
                .ifPresent(existing -> {
                    if (existing.isCompleted()) {
                        throw new PaymentAlreadyCompletedException(existing.id());
                    }
                    // PENDING/FAILED 상태도 중복으로 간주 (재결제 시도 방지)
                    log.warn("Payment already exists for reservation: reservationId={}, status={}",
                            command.reservationId(), existing.status());
                    throw new PaymentAlreadyCompletedException(existing.id());
                });

        try {
            // 결제 생성 (PENDING 상태로 먼저 DB 저장)
            Payment payment = Payment.create(
                    command.reservationId(),
                    command.userId(),
                    command.amount(),
                    command.paymentMethod()
            );

            Payment pendingPayment = paymentRepository.save(payment);
            log.info("Payment created in PENDING state: paymentId={}", pendingPayment.id());

            return pendingPayment;

        } catch (DataIntegrityViolationException e) {
            // 2차 방어: DB Unique Constraint 위반 (동시 요청 시)
            log.error("Duplicate payment detected by DB constraint: reservationId={}",
                    command.reservationId(), e);

            // 이미 생성된 결제를 조회하여 예외 발생
            Payment existing = paymentRepository.findByReservationId(command.reservationId())
                    .orElseThrow(() -> new personal.ai.common.exception.BusinessException(
                            personal.ai.common.exception.ErrorCode.CONFLICT,
                            "중복 결제 감지"));

            throw new PaymentAlreadyCompletedException(existing.id());
        }
    }

    /**
     * 결제 성공 처리 (Tx2)
     */
    @Transactional
    protected Payment handlePaymentSuccess(Payment pendingPayment, String concertId) {
        // 결제 성공: PENDING -> COMPLETED
        Payment completedPayment = pendingPayment.complete();
        Payment savedPayment = paymentRepository.save(completedPayment);

        log.info("Payment completed: paymentId={}, reservationId={}",
                savedPayment.id(), savedPayment.reservationId());

        // Outbox Pattern: 이벤트를 DB에 저장 (같은 트랜잭션 내)
        // 스케줄러가 주기적으로 발행 처리
        savePaymentCompletedEventToOutbox(savedPayment, concertId);

        return savedPayment;
    }

    /**
     * 결제 완료 이벤트를 Outbox에 저장
     * Transactional Outbox Pattern: DB 트랜잭션 커밋 후 스케줄러가 발행
     */
    private void savePaymentCompletedEventToOutbox(Payment payment, String concertId) {
        try {
            // 이벤트 DTO 생성
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    UUID.randomUUID().toString(),
                    concertId,
                    payment.userId().toString(),
                    payment.reservationId().toString(),
                    payment.id().toString(),
                    payment.amount().longValue(),
                    Instant.now()
            );

            // JSON 직렬화
            String payload = objectMapper.writeValueAsString(event);

            // Outbox 테이블에 저장
            PaymentOutboxEventEntity outboxEvent = PaymentOutboxEventEntity.create(
                    "PAYMENT",
                    payment.id(),
                    "PAYMENT_COMPLETED",
                    payload
            );

            paymentOutboxRepository.save(outboxEvent.toDomain());
            log.info("Payment completed event saved to outbox: paymentId={}", payment.id());

        } catch (Exception e) {
            log.error("Failed to save payment completed event to outbox: paymentId={}", payment.id(), e);
            throw new RuntimeException("Failed to save payment event to outbox", e);
        }
    }

    /**
     * 결제 실패 처리 (Tx3)
     */
    @Transactional
    protected void handlePaymentFailure(Payment pendingPayment, Reservation reservation, Long userId) {
        // 결제 실패: PENDING -> FAILED
        Payment failedPayment = pendingPayment.fail();
        paymentRepository.save(failedPayment);

        // 좌석 선점 해제 (Redis seat lock 삭제)
        seatLockRepository.unlock(reservation.seatId(), userId);
        log.info("Seat lock released due to payment failure: seatId={}, userId={}",
                reservation.seatId(), userId);

        // 예약 취소 처리
        Reservation cancelledReservation = reservation.cancel();
        reservationRepository.save(cancelledReservation);

        log.warn("Payment failed: paymentId={}, reservationId={}",
                failedPayment.id(), failedPayment.reservationId());
    }

    /**
     * Mock 결제 처리 (트랜잭션 범위 밖)
     * 80% 성공, 20% 실패, 500ms~1s 딜레이
     */
    private boolean processMockPayment(Long userId, Long reservationId, Long amount) {
        log.debug("Calling PaymentMockService: userId={}, reservationId={}, amount={}",
                userId, reservationId, amount);
        return paymentMockService.processPayment(userId, reservationId, amount);
    }
}
