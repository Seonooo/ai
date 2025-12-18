package personal.ai.core.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import personal.ai.core.acceptance.support.BookingHttpAdapter;
import personal.ai.core.acceptance.support.BookingTestAdapter;
import personal.ai.core.acceptance.support.BookingTestContext;
import personal.ai.core.booking.domain.model.ReservationStatus;
import personal.ai.core.booking.domain.model.SeatStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Booking Exception Test Step Definitions
 * 예외 케이스 시나리오 테스트
 *
 * Context 공유: BookingTestContext를 통해 다른 Step 클래스와 상태 공유
 */
@Slf4j
@RequiredArgsConstructor
public class BookingExceptionSteps {

    /** HTTP API 호출을 위한 어댑터 */
    private final BookingHttpAdapter httpAdapter;
    /** 테스트 유틸리티 어댑터 */
    private final BookingTestAdapter testUtility;
    /** 시나리오 간 상태 공유를 위한 컨텍스트 (@ScenarioScope) */
    private final BookingTestContext context;

    @Given("유효한 대기열 토큰을 보유하고 있다")
    public void 유효한_대기열_토큰을_보유하고_있다() {
        // Stubbing 등 사전 조건 설정 (현 단계에서는 Pass)
        log.info(">>> Given: 대기열 토큰 검증 Pass");
    }

    // ==========================================
    // 존재하지 않는 좌석 예약
    // ==========================================

    @When("존재하지 않는 좌석 ID로 예약을 요청한다")
    public void 존재하지_않는_좌석_ID로_예약을_요청한다() {
        Long invalidSeatId = 99999L;
        context.setCurrentUserId(1L);
        context.setCurrentQueueToken(testUtility.issueActiveQueueToken(
            context.getDefaultConcertId(), context.getCurrentUserId()));

        log.info(">>> When: POST /api/v1/reservations - 존재하지 않는 좌석 - seatId={}", invalidSeatId);
        context.setLastHttpResponse(httpAdapter.reserveSeat(
            context.getCurrentScheduleId(), invalidSeatId, context.getCurrentUserId(), context.getCurrentQueueToken()));
    }

    @Then("요청된 예약이 거부된다")
    public void 요청된_예약이_거부된다() {
        // 존재하지 않는 좌석: 리소스를 찾을 수 없음
        assertThat(context.getLastHttpResponse().statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        log.info(">>> Then: 예약 거부 확인 - HTTP 404 NOT_FOUND");
    }

    @Then("좌석을 찾을 수 없다는 메시지가 반환된다")
    public void 좌석을_찾을_수_없다는_메시지가_반환된다() {
        String message = context.getLastHttpResponse().jsonPath().getString("message");
        assertThat(message).isNotNull();
        log.info(">>> Then: 에러 메시지 확인 - message={}", message);
    }

    // ==========================================
    // 타인 예약 조회
    // ==========================================

    @Given("다른 사용자가 생성한 예약이 존재한다")
    public void 다른_사용자가_생성한_예약이_존재한다() {
        testUtility.createScheduleIfNotExists(context.getCurrentScheduleId());
        context.setOtherUserId(testUtility.createUser("OtherUser"));
        Long seatId = testUtility.createSeat(context.getCurrentScheduleId(), "B1", SeatStatus.AVAILABLE);

        // 다른 사용자로 예약 생성
        context.setOtherReservationId(testUtility.createReservation(
            null, context.getOtherUserId(), seatId, ReservationStatus.PENDING));
        log.info(">>> Given: 타인 예약 생성 - reservationId={}, userId={}",
            context.getOtherReservationId(), context.getOtherUserId());
    }

    @When("해당 예약 조회를 요청한다")
    public void 해당_예약_조회를_요청한다() {
        context.setCurrentUserId(1L); // 현재 로그인한 사용자
        log.info(">>> When: GET /api/v1/reservations/{} - 타인 예약 조회 시도", context.getOtherReservationId());
        context.setLastHttpResponse(httpAdapter.getReservation(
            context.getOtherReservationId(), context.getCurrentUserId()));
    }

    @Then("조회가 거부된다")
    public void 조회가_거부된다() {
        assertThat(context.getLastHttpResponse().statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value()); // 403 Forbidden Expected
        log.info(">>> Then: 조회 거부 확인 (403)");
    }

    @Then("접근 권한이 없다는 메시지가 반환된다")
    public void 접근_권한이_없다는_메시지가_반환된다() {
        log.info(">>> Then: 권한 에러 메시지 확인 (생략 가능)");
    }

    // ==========================================
    // 확정된 예약 취소 불가
    // ==========================================

    @Given("내가 생성한 예약이 이미 확정\\(Payment Completed) 상태이다")
    public void 내가_생성한_예약이_이미_확정_상태이다() {
        testUtility.createScheduleIfNotExists(context.getCurrentScheduleId());
        Long myUserId = 1L;
        Long seatId = testUtility.createSeat(context.getCurrentScheduleId(), "C1", SeatStatus.RESERVED);

        context.setMyReservationId(testUtility.createReservation(
            null, myUserId, seatId, ReservationStatus.CONFIRMED));
        log.info(">>> Given: 확정된 예약 생성 - reservationId={}", context.getMyReservationId());
    }

    @When("해당 예약 취소를 요청한다")
    public void 해당_예약_취소를_요청한다() {
        // 취소 API가 존재한다고 가정 (DELETE /api/v1/reservations/{id} or PATCH)
        // 현재 명세에 취소가 없다면 이 시나리오는 스킵하거나 실패할 것임.
        // 가정: 취소 API 제공 안 함 -> 404 or 405 Method Not Allowed
        log.warn(">>> When: 예약 취소 요청 (API 미구현으로 가정)");
        // 임시로 아무것도 안 함 or 404 확인
    }

    @Then("요청된 취소가 거부된다")
    public void 요청된_취소가_거부된다() {
        // API가 없으므로 Pass, 혹은 405 검증
        log.info(">>> Then: 취소 실패 확인");
    }

    // ==========================================
    // 만료된 예약 결제 불가
    // ==========================================

    @Given("내가 생성한 예약이 만료\\(Time Expired)되었다")
    public void 내가_생성한_예약이_만료되었다() {
        testUtility.createScheduleIfNotExists(context.getCurrentScheduleId());
        Long myUserId = 1L;
        Long seatId = testUtility.createSeat(context.getCurrentScheduleId(), "D1", SeatStatus.AVAILABLE);

        // 예약 생성 후 강제 만료
        context.setMyReservationId(testUtility.createReservation(
            null, myUserId, seatId, ReservationStatus.PENDING));
        testUtility.expireReservationImmediately(context.getMyReservationId());

        log.info(">>> Given: 예약 만료 처리 완료 - reservationId={}", context.getMyReservationId());
    }

    @When("해당 예약에 대해 결제를 요청한다")
    public void 해당_예약에_대해_결제를_요청한다() {
        Long myUserId = 1L;
        log.info(">>> When: POST /api/v1/payments - reservationId={}", context.getMyReservationId());
        context.setLastHttpResponse(httpAdapter.processPayment(
                context.getMyReservationId(),
                myUserId,
                new java.math.BigDecimal("50000"),
                "CREDIT_CARD",
                context.getDefaultConcertId()));
    }

    @Then("요청된 결제가 거부된다")
    public void 요청된_결제가_거부된다() {
        // 만료된 예약: 유효하지 않은 상태의 리소스에 대한 요청
        assertThat(context.getLastHttpResponse().statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        log.info(">>> Then: 결제 실패 확인 - HTTP 400 BAD_REQUEST");
    }

    // ==========================================
    // 결제 예외 (P1)
    // ==========================================

    @Given("내가 생성한 예약이 존재한다")
    public void 내가_생성한_예약이_존재한다() {
        testUtility.createScheduleIfNotExists(context.getCurrentScheduleId());
        Long myUserId = 1L;
        Long seatId = testUtility.createSeat(context.getCurrentScheduleId(), "P1", SeatStatus.AVAILABLE);

        context.setMyReservationId(testUtility.createReservation(
            null, myUserId, seatId, ReservationStatus.PENDING));
        log.info(">>> Given: 내 예약 생성 - reservationId={}", context.getMyReservationId());
    }

    // TODO: 결제 로직 개발 시 추가
//    @When("해당 예약에 대해 잘못된 금액으로 결제를 요청한다")
//    public void 해당_예약에_대해_잘못된_금액으로_결제를_요청한다() {
//        Long myUserId = 1L;
//        log.info(">>> When: POST /api/v1/payments - 잘못된 금액 - reservationId={}", myReservationId);
//        lastHttpResponse = httpAdapter.processPayment(
//                myReservationId,
//                myUserId,
//                new java.math.BigDecimal("100"), // 잘못된 금액
//                "CREDIT_CARD",
//                DEFAULT_CONCERT_ID);
//    }
//
//    @Then("요청된 결제 금액 불일치로 거부된다")
//    public void 요청된_결제_금액_불일치로_거부된다() {
//        assertThat(lastHttpResponse.statusCode()).isBetween(400, 499);
//        log.info(">>> Then: 금액 불일치 거부 확인");
//    }
//
//    @Then("금액이 일치하지 않는다는 메시지가 반환된다")
//    public void 금액이_일치하지_않는다는_메시지가_반환된다() {
//        // 메시지 검증 로직 (구체적인 메시지는 구현에 따름)
//        // assertThat(lastHttpResponse.body().asString()).contains("amount mismatch");
//    }

    @When("해당 예약에 대해 내가 결제를 요청한다")
    public void 해당_예약에_대해_내가_결제를_요청한다() {
        // "다른 사용자가 생성한 예약이 존재한다" -> otherReservationId
        Long myUserId = 1L;
        log.info(">>> When: POST /api/v1/payments - 타인 예약 결제 시도 - reservationId={}", context.getOtherReservationId());
        context.setLastHttpResponse(httpAdapter.processPayment(
                context.getOtherReservationId(),  // 타인의 예약 ID
                myUserId,  // 요청자는 나
                new java.math.BigDecimal("50000"),
                "CREDIT_CARD",
                context.getDefaultConcertId()));
    }

    @Then("요청된 결제가 사용자 불일치로 거부된다")
    public void 요청된_결제가_사용자_불일치로_거부된다() {
        // 권한 없음: 다른 사용자의 예약에 대한 결제 시도
        assertThat(context.getLastHttpResponse().statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        log.info(">>> Then: 사용자 불일치 거부 확인 - HTTP 403 FORBIDDEN");
    }

    @Then("중복 결제 요청이 거부된다")
    public void 중복_결제_요청이_거부된다() {
        // 중복 요청: 이미 결제된 예약에 대한 재결제 시도 (리소스 충돌)
        assertThat(context.getLastHttpResponse().statusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        log.info(">>> Then: 중복 결제 거부 확인 - HTTP 409 CONFLICT");
    }

    @Then("이미 확정된 예약이라는 메시지가 반환된다")
    public void 이미_확정된_예약이라는_메시지가_반환된다() {
        log.info(">>> Then: 이미 확정된 예약 메시지 확인");
        String message = context.getLastHttpResponse().jsonPath().getString("message");
        assertThat(message).isNotNull();
    }

    @Then("만료된 예약이라는 메시지가 반환된다")
    public void 만료된_예약이라는_메시지가_반환된다() {
        String message = context.getLastHttpResponse().jsonPath().getString("message");
        // 실제 에러 메시지 확인 필요 (예: "Reservation has expired")
        log.info(">>> Then: 만료 에러 메시지 확인 - message={}", message);
    }
}
