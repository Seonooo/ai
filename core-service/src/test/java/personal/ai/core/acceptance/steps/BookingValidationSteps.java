package personal.ai.core.acceptance.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import personal.ai.common.exception.ErrorCode;
import personal.ai.core.acceptance.support.BookingTestAdapter;
import personal.ai.core.acceptance.support.BookingTestContext;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Booking Validation Test Step Definitions
 * 입력값 검증 테스트를 위한 Step Definitions
 *
 * Context 공유: BookingTestContext를 통해 다른 Step 클래스와 상태 공유
 */
@Slf4j
@RequiredArgsConstructor
public class BookingValidationSteps {

    private static final String BASE_URI = "http://localhost";
    private final Environment environment;
    private final BookingTestAdapter testUtility;
    /** 시나리오 간 상태 공유를 위한 컨텍스트 (@ScenarioScope) */
    private final BookingTestContext context;

    /**
     * 서버 포트 가져오기
     */
    private int getPort() {
        return environment.getProperty("local.server.port", Integer.class, 8080);
    }

    // ==========================================
    // 좌석 조회 Validation
    // ==========================================

    @When("scheduleId 없이 좌석 조회를 요청한다")
    public void scheduleId_없이_좌석_조회를_요청한다() {
        log.info(">>> When: GET /api/v1/schedules/{}/seats - 잘못된 scheduleId 형식", "invalid");

        // 숫자가 아닌 문자열 전달 → @PathVariable Long 타입 변환 실패 → 400 Bad Request
        // 빈 문자열("")은 경로 매칭 실패로 404를 발생시키므로 "invalid" 사용
        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                .header("X-User-Id", 1L)
                .header("X-Queue-Token", "valid-token")
                .when()
                .get("/api/v1/schedules/{scheduleId}/seats", "invalid")); // 숫자가 아닌 값

        log.info(">>> Response status: {}", context.getLastHttpResponse().statusCode());
    }

    @When("대기열 토큰 없이 좌석 조회를 요청한다")
    public void 대기열_토큰_없이_좌석_조회를_요청한다() {
        log.info(">>> When: GET /api/v1/schedules/1/seats - 대기열 토큰 누락");

        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                .header("X-User-Id", 1L)
                // X-Queue-Token 헤더 누락
                .when()
                .get("/api/v1/schedules/{scheduleId}/seats", 1L));
    }

    @When("잘못된 대기열 토큰으로 좌석 조회를 요청한다")
    public void 잘못된_대기열_토큰으로_좌석_조회를_요청한다() {
        log.info(">>> When: GET /api/v1/schedules/1/seats - 잘못된 대기열 토큰");

        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                .header("X-User-Id", 1L)
                .header("X-Queue-Token", "invalid-token-12345")
                .when()
                .get("/api/v1/schedules/{scheduleId}/seats", 1L));
    }

    // ==========================================
    // 예약 생성 Validation
    // ==========================================

    @When("scheduleId 없이 예약을 요청한다")
    public void scheduleId_없이_예약을_요청한다() {
        log.info(">>> When: POST /api/v1/reservations - scheduleId 누락");

        Map<String, Object> body = new HashMap<>();
        // scheduleId 누락
        body.put("seatId", 1L);

        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                .header("X-User-Id", 1L)
                .header("X-Queue-Token", "valid-token")
                .body(body)
                .when()
                .post("/api/v1/reservations"));
    }

    @When("seatId 없이 예약을 요청한다")
    public void seatId_없이_예약을_요청한다() {
        log.info(">>> When: POST /api/v1/reservations - seatId 누락");

        Map<String, Object> body = new HashMap<>();
        body.put("scheduleId", 1L);
        // seatId 누락

        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                .header("X-User-Id", 1L)
                .header("X-Queue-Token", "valid-token")
                .body(body)
                .when()
                .post("/api/v1/reservations"));
    }

    @When("userId 헤더 없이 예약을 요청한다")
    public void userId_헤더_없이_예약을_요청한다() {
        log.info(">>> When: POST /api/v1/reservations - userId 헤더 누락");

        Map<String, Object> body = new HashMap<>();
        body.put("scheduleId", 1L);
        body.put("seatId", 1L);

        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                // X-User-Id 헤더 누락
                .header("X-Queue-Token", "valid-token")
                .body(body)
                .when()
                .post("/api/v1/reservations"));
    }

    @When("대기열 토큰 없이 예약을 요청한다")
    public void 대기열_토큰_없이_예약을_요청한다() {
        log.info(">>> When: POST /api/v1/reservations - 대기열 토큰 누락");

        Map<String, Object> body = new HashMap<>();
        body.put("scheduleId", 1L);
        body.put("seatId", 1L);

        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                .header("X-User-Id", 1L)
                // X-Queue-Token 헤더 누락
                .body(body)
                .when()
                .post("/api/v1/reservations"));
    }

    // ==========================================
    // 결제 Validation
    // ==========================================

    @When("reservationId 없이 결제를 요청한다")
    public void reservationId_없이_결제를_요청한다() {
        log.info(">>> When: POST /api/v1/payments - reservationId 누락");

        Map<String, Object> body = new HashMap<>();
        // reservationId 누락
        body.put("userId", 1L);
        body.put("amount", new BigDecimal("50000"));
        body.put("paymentMethod", "CREDIT_CARD");
        body.put("concertId", context.getDefaultConcertId());

        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/payments"));
    }

    @When("amount 없이 결제를 요청한다")
    public void amount_없이_결제를_요청한다() {
        log.info(">>> When: POST /api/v1/payments - amount 누락");

        // acceptanceSteps에서 reservationId 가져오기
        Long reservationId = getReservationIdFromContext();

        Map<String, Object> body = new HashMap<>();
        body.put("reservationId", reservationId);
        body.put("userId", 1L);
        // amount 누락
        body.put("paymentMethod", "CREDIT_CARD");
        body.put("concertId", context.getDefaultConcertId());

        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/payments"));
    }

    @When("음수 금액으로 결제를 요청한다")
    public void 음수_금액으로_결제를_요청한다() {
        log.info(">>> When: POST /api/v1/payments - 음수 금액");

        Long reservationId = getReservationIdFromContext();

        Map<String, Object> body = new HashMap<>();
        body.put("reservationId", reservationId);
        body.put("userId", 1L);
        body.put("amount", new BigDecimal("-1000")); // 음수 금액
        body.put("paymentMethod", "CREDIT_CARD");
        body.put("concertId", context.getDefaultConcertId());

        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/payments"));
    }

    @When("paymentMethod 없이 결제를 요청한다")
    public void paymentMethod_없이_결제를_요청한다() {
        log.info(">>> When: POST /api/v1/payments - paymentMethod 누락");

        Long reservationId = getReservationIdFromContext();

        Map<String, Object> body = new HashMap<>();
        body.put("reservationId", reservationId);
        body.put("userId", 1L);
        body.put("amount", new BigDecimal("50000"));
        // paymentMethod 누락
        body.put("concertId", context.getDefaultConcertId());

        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/payments"));
    }

    @When("userId 헤더 없이 결제를 요청한다")
    public void userId_헤더_없이_결제를_요청한다() {
        log.info(">>> When: POST /api/v1/payments - userId 헤더 누락");

        Long reservationId = getReservationIdFromContext();

        Map<String, Object> body = new HashMap<>();
        body.put("reservationId", reservationId);
        body.put("userId", 1L);
        body.put("amount", new BigDecimal("50000"));
        body.put("paymentMethod", "CREDIT_CARD");
        body.put("concertId", context.getDefaultConcertId());

        // X-User-Id 헤더는 결제 API에서 사용하지 않을 수도 있음
        // body의 userId를 누락시키거나, 헤더가 필요한 경우 헤더를 누락
        context.setLastHttpResponse(RestAssured.given()
                .baseUri(BASE_URI)
                .port(getPort())
                .contentType(ContentType.JSON)
                // X-User-Id 헤더 누락 (만약 필요한 경우)
                .body(body)
                .when()
                .post("/api/v1/payments"));
    }

    // ==========================================
    // Then: 검증
    // ==========================================

    @Then("요청이 거부된다")
    public void 요청이_거부된다() {
        log.info(">>> Then: 요청 거부 확인 - HTTP 400 Bad Request");
        assertThat(context.getLastHttpResponse().statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Then("인증이 거부된다")
    public void 인증이_거부된다() {
        log.info(">>> Then: 인증 거부 확인 - HTTP 401 Unauthorized");
        assertThat(context.getLastHttpResponse().statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Then("잘못된 입력값이라는 메시지가 반환된다")
    public void 잘못된_입력값이라는_메시지가_반환된다() {
        log.info(">>> Then: 잘못된 입력값 메시지 확인");
        String code = context.getLastHttpResponse().jsonPath().getString("code");
        String message = context.getLastHttpResponse().jsonPath().getString("message");

        assertThat(code).isEqualTo(ErrorCode.INVALID_INPUT.getCode());
        // Validation 예외는 다양한 메시지를 반환할 수 있으므로 기본 메시지를 포함하는지 확인
        assertThat(message).containsAnyOf(
                ErrorCode.INVALID_INPUT.getMessage(),
                "입력값이 유효하지 않습니다",
                "필수 파라미터가 누락되었습니다"
        );
        log.debug("Error code: {}, message: {}", code, message);
    }

    @Then("대기열 토큰이 필요하다는 메시지가 반환된다")
    public void 대기열_토큰이_필요하다는_메시지가_반환된다() {
        log.info(">>> Then: 대기열 토큰 필요 메시지 확인");
        String code = context.getLastHttpResponse().jsonPath().getString("code");
        String message = context.getLastHttpResponse().jsonPath().getString("message");

        // 토큰이 누락된 경우 INVALID_INPUT, UNAUTHORIZED, 또는 QUEUE_TOKEN_NOT_FOUND일 수 있음
        assertThat(code).isIn(
                ErrorCode.INVALID_INPUT.getCode(),
                ErrorCode.UNAUTHORIZED.getCode(),
                ErrorCode.QUEUE_TOKEN_NOT_FOUND.getCode()
        );
        // 각 에러 코드에 해당하는 메시지 중 하나를 포함해야 함
        assertThat(message).containsAnyOf(
                ErrorCode.INVALID_INPUT.getMessage(),
                ErrorCode.UNAUTHORIZED.getMessage(),
                ErrorCode.QUEUE_TOKEN_NOT_FOUND.getMessage(),
                "토큰",
                "대기열"
        );
        log.debug("Error code: {}, message: {}", code, message);
    }

    @Then("유효하지 않은 토큰이라는 메시지가 반환된다")
    public void 유효하지_않은_토큰이라는_메시지가_반환된다() {
        log.info(">>> Then: 유효하지 않은 토큰 메시지 확인");
        String code = context.getLastHttpResponse().jsonPath().getString("code");
        String message = context.getLastHttpResponse().jsonPath().getString("message");

        assertThat(code).isEqualTo(ErrorCode.QUEUE_TOKEN_INVALID.getCode());
        assertThat(message).isEqualTo(ErrorCode.QUEUE_TOKEN_INVALID.getMessage());
        log.debug("Error code: {}, message: {}", code, message);
    }

    @Then("사용자 인증이 필요하다는 메시지가 반환된다")
    public void 사용자_인증이_필요하다는_메시지가_반환된다() {
        log.info(">>> Then: 사용자 인증 필요 메시지 확인");
        String code = context.getLastHttpResponse().jsonPath().getString("code");
        String message = context.getLastHttpResponse().jsonPath().getString("message");

        // 사용자 인증 관련 에러는 UNAUTHORIZED 또는 INVALID_CREDENTIALS일 수 있음
        assertThat(code).isIn(
                ErrorCode.UNAUTHORIZED.getCode(),
                ErrorCode.INVALID_CREDENTIALS.getCode(),
                ErrorCode.INVALID_INPUT.getCode()
        );
        // 각 에러 코드에 해당하는 메시지 중 하나를 포함해야 함
        assertThat(message).containsAnyOf(
                ErrorCode.UNAUTHORIZED.getMessage(),
                ErrorCode.INVALID_CREDENTIALS.getMessage(),
                ErrorCode.INVALID_INPUT.getMessage(),
                "인증",
                "사용자"
        );
        log.debug("Error code: {}, message: {}", code, message);
    }

    @Then("잘못된 금액이라는 메시지가 반환된다")
    public void 잘못된_금액이라는_메시지가_반환된다() {
        log.info(">>> Then: 잘못된 금액 메시지 확인");
        String code = context.getLastHttpResponse().jsonPath().getString("code");
        String message = context.getLastHttpResponse().jsonPath().getString("message");

        assertThat(code).isEqualTo(ErrorCode.INVALID_INPUT.getCode());
        // 금액 검증 실패 시 다양한 메시지가 나올 수 있음
        assertThat(message).containsAnyOf(
                ErrorCode.INVALID_INPUT.getMessage(),
                "금액",
                "amount",
                "양수"
        );
        log.debug("Error code: {}, message: {}", code, message);
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    /**
     * 컨텍스트에서 reservationId 가져오기
     */
    private Long getReservationIdFromContext() {
        if (context.getCurrentReservationId() == null) {
            log.warn(">>> currentReservationId is null, using default: 1L");
            return 1L; // fallback
        }
        return context.getCurrentReservationId();
    }
}
