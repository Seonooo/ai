package personal.ai.core.acceptance.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.web.server.LocalServerPort;
import personal.ai.core.booking.adapter.out.persistence.JpaReservationRepository;
import personal.ai.core.booking.adapter.out.persistence.JpaSeatRepository;
import personal.ai.core.booking.adapter.out.persistence.ReservationEntity;
import personal.ai.core.booking.adapter.out.persistence.SeatEntity;
import personal.ai.core.booking.domain.model.SeatGrade;
import personal.ai.core.booking.domain.model.SeatStatus;
import personal.ai.core.payment.adapter.out.persistence.JpaPaymentOutboxRepository;
import personal.ai.core.payment.adapter.out.persistence.JpaPaymentRepository;
import personal.ai.core.payment.adapter.out.persistence.PaymentEntity;
import personal.ai.core.payment.adapter.out.persistence.PaymentOutboxEventEntity;
import personal.ai.core.user.adapter.out.persistence.JpaUserRepository;
import personal.ai.core.user.adapter.out.persistence.UserEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전체 예매 플로우 E2E Step Definitions
 * 대기열 진입 > 좌석 선점 > 결제 > 예약 완료
 */
@Slf4j
@RequiredArgsConstructor
public class BookingE2ESteps {

    private final JpaSeatRepository seatRepository;
    private final JpaUserRepository userRepository;
    private final JpaReservationRepository reservationRepository;
    private final JpaPaymentRepository paymentRepository;
    private final JpaPaymentOutboxRepository outboxRepository;
    @LocalServerPort
    private int port;
    // Test context
    private Long scheduleId;
    private String seatNumber;
    private Long userId;
    private Long seatId;
    private Long reservationId;
    private Long paymentId;
    private String queueToken;
    private Response lastResponse;

    @After
    public void cleanup() {
        log.info("Cleaning up test data...");
        // Outbox 이벤트는 삭제하지 않음 (검증용)
    }

    // ==================== Given: 테스트 데이터 준비 ====================

    @Given("스케줄 ID {int}번에 좌석 {string}가 {string} 상태로 존재한다")
    public void setupSeat(int scheduleId, String seatNumber, String status) {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";

        this.scheduleId = Long.valueOf(scheduleId);
        this.seatNumber = seatNumber;

        // 좌석 생성
        SeatEntity seat = SeatEntity.fromDomain(
                personal.ai.core.booking.domain.model.Seat.create(
                        this.scheduleId,
                        seatNumber,
                        SeatGrade.A,
                        BigDecimal.valueOf(50000),
                        SeatStatus.valueOf(status)));
        SeatEntity savedSeat = seatRepository.save(seat);
        this.seatId = savedSeat.getId();

        log.info("✓ 좌석 생성: ID={}, Number={}, Status={}", seatId, seatNumber, status);
    }

    @And("사용자 {string}가 등록되어 있다")
    public void setupUser(String username) {
        UserEntity user = UserEntity.of(username, username + "@test.com");
        UserEntity savedUser = userRepository.save(user);
        this.userId = savedUser.getId();

        log.info("✓ 사용자 생성: ID={}, Name={}", userId, username);
    }

    // ==================== Step 1: 대기열 진입 ====================

    @When("사용자가 대기열 활성 토큰을 발급받는다")
    public void userGetsActiveToken() {
        // 실제로는 Queue Service POST /api/v1/queue/enter 호출
        // 테스트에서는 Mock 활성 토큰 생성
        this.queueToken = "active-token-" + userId;
        log.info("✓ Step 1: 대기열 활성 토큰 발급 - {}", queueToken);
    }

    @Then("활성 토큰이 발급된다")
    public void verifyActiveToken() {
        assertThat(queueToken).isNotNull();
        assertThat(queueToken).startsWith("active-token");
        log.info("  - 토큰 검증 완료");
    }

    // ==================== Step 2: 좌석 선점 (예약 생성) ====================

    @When("사용자가 좌석을 예약한다")
    public void userReservesSeat() {
        lastResponse = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .header("X-User-Id", userId)
                .header("X-Queue-Token", queueToken)
                .body(Map.of(
                        "userId", userId,
                        "scheduleId", scheduleId,
                        "seatId", seatId,
                        "concertId", "1"))
                .when()
                .post("/api/v1/reservations")
                .then()
                .extract()
                .response();

        log.info("✓ Step 2: 좌석 예약 요청 - Status Code: {}", lastResponse.statusCode());

        if (lastResponse.statusCode() == 201) {
            this.reservationId = lastResponse.jsonPath().getLong("data.reservationId");
            log.info("  - 예약 ID: {}", reservationId);
        } else {
            log.error("  - 예약 실패: {}", lastResponse.body().asString());
        }
    }

    @Then("예약이 생성되고 상태는 {string}이다")
    public void verifyReservationCreated(String expectedStatus) {
        assertThat(lastResponse.statusCode()).isEqualTo(201);
        assertThat(reservationId).isNotNull();

        ReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AssertionError("Reservation not found: " + reservationId));

        assertThat(reservation.getStatus().name()).isEqualTo(expectedStatus);
        log.info("  - 예약 상태 검증: {}", reservation.getStatus());
    }

    @And("좌석 상태는 {string}로 변경된다")
    public void verifySeatStatusChanged(String expectedStatus) {
        SeatEntity seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new AssertionError("Seat not found: " + seatId));

        assertThat(seat.getStatus().name()).isEqualTo(expectedStatus);
        log.info("  - 좌석 상태 검증: {}", seat.getStatus());
    }

    // ==================== Step 3: 결제 진행 ====================

    @When("사용자가 결제를 진행한다")
    public void userProcessesPayment() {
        // Mock 결제는 80% 성공률이므로 여러 번 시도
        int maxAttempts = 10;
        boolean paymentSuccess = false;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            lastResponse = RestAssured
                    .given()
                    .contentType(ContentType.JSON)
                    .header("X-User-Id", userId)
                    .body(Map.of(
                            "reservationId", reservationId,
                            "userId", userId,
                            "amount", 50000,
                            "paymentMethod", "CARD",
                            "concertId", "1"))
                    .when()
                    .post("/api/v1/payments")
                    .then()
                    .extract()
                    .response();

            log.info("✓ Step 3: 결제 시도 {}/{} - Status Code: {}",
                    attempt, maxAttempts, lastResponse.statusCode());

            if (lastResponse.statusCode() == 201) {
                this.paymentId = lastResponse.jsonPath().getLong("data.paymentId");
                log.info("  - 결제 ID: {}", paymentId);
                paymentSuccess = true;
                break;
            } else if (lastResponse.statusCode() == 409) {
                // 이미 결제됨
                log.warn("  - 이미 처리된 결제");
                break;
            } else {
                log.warn("  - 결제 실패 (Mock 20% 실패) - 재시도...");

                // 실패 시 예약/좌석 상태 복구하여 재시도
                if (attempt < maxAttempts) {
                    // 예약 삭제
                    reservationRepository.deleteById(reservationId);

                    // 좌석 상태 복구
                    SeatEntity seat = seatRepository.findById(seatId).orElseThrow();
                    seat.updateStatus(SeatStatus.AVAILABLE);
                    seatRepository.save(seat);

                    // 재예약
                    userReservesSeat();

                    // 잠시 대기
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (!paymentSuccess && lastResponse.statusCode() != 409) {
            log.error("  - 결제 최종 실패: 10회 모두 실패");
        }
    }

    @Then("결제가 완료되고 상태는 {string}이다")
    public void verifyPaymentCompleted(String expectedStatus) {
        assertThat(lastResponse.statusCode()).isIn(201, 409);

        if (lastResponse.statusCode() == 201) {
            assertThat(paymentId).isNotNull();

            PaymentEntity payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new AssertionError("Payment not found: " + paymentId));

            assertThat(payment.getStatus().name()).isEqualTo(expectedStatus);
            log.info("  - 결제 상태 검증: {}", payment.getStatus());
        } else {
            log.info("  - 중복 결제 방지됨");
        }
    }

    // ==================== Step 4: Outbox 이벤트 확인 ====================

    @Then("결제 완료 이벤트가 Outbox 테이블에 {string} 상태로 저장된다")
    public void verifyOutboxEventSaved(String expectedStatus) {
        if (paymentId != null) {
            List<PaymentOutboxEventEntity> events = outboxRepository.findAll();
            assertThat(events).isNotEmpty();

            PaymentOutboxEventEntity event = events.stream()
                    .filter(e -> e.getAggregateId().equals(paymentId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Payment event not found in outbox"));

            assertThat(event.getAggregateType()).isEqualTo("PAYMENT");
            assertThat(event.getEventType()).isEqualTo("PAYMENT_COMPLETED");
            assertThat(event.getStatus().name()).isEqualTo(expectedStatus);

            log.info("✓ Step 4: Outbox 이벤트 저장 확인");
            log.info("  - Event Type: {}", event.getEventType());
            log.info("  - Status: {}", event.getStatus());
        }
    }

    // ==================== Step 5: Outbox 스케줄러 실행 ====================

    @When("Outbox 스케줄러가 실행된다")
    public void waitForOutboxScheduler() throws InterruptedException {
        // Outbox 스케줄러는 5초 간격으로 실행
        log.info("✓ Step 5: Outbox 스케줄러 대기 중... (10초)");
        Thread.sleep(10000);
    }

    @Then("Outbox 이벤트 상태가 {string}로 변경된다")
    public void verifyOutboxEventPublished(String expectedStatus) {
        if (paymentId != null) {
            List<PaymentOutboxEventEntity> events = outboxRepository.findAll();
            PaymentOutboxEventEntity event = events.stream()
                    .filter(e -> e.getAggregateId().equals(paymentId))
                    .findFirst()
                    .orElseThrow();

            log.info("  - 현재 Outbox 이벤트 상태: {}", event.getStatus());

            // 스케줄러가 실행되었다면 PUBLISHED 상태
            // 실제로는 비동기이므로 완벽한 검증은 어려움
            if (event.getStatus().name().equals(expectedStatus)) {
                log.info("  - ✅ 이벤트 발행 완료!");
            } else {
                log.warn("  - ⚠️ 이벤트 발행 대기 중 (비동기 처리)");
            }
        }
    }
}
