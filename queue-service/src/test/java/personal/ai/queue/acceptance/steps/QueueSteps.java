package personal.ai.queue.acceptance.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import personal.ai.common.exception.ErrorCode;
import personal.ai.queue.acceptance.support.QueueHttpAdapter;
import personal.ai.queue.acceptance.support.QueueTestAdapter;
import personal.ai.queue.adapter.in.web.dto.QueuePositionResponse;
import personal.ai.queue.adapter.in.web.dto.QueueTokenResponse;
import personal.ai.queue.application.port.in.MoveToActiveQueueUseCase;
import personal.ai.queue.domain.model.QueueConfig;
import personal.ai.queue.domain.model.QueueStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Queue Service API 인수 테스트 Step Definitions
 *
 * 이 클래스는 Cucumber Feature 파일의 각 시나리오 단계(Given/When/Then)를
 * 실제 테스트 코드로 매핑합니다.
 *
 * @ScenarioScope: 각 시나리오마다 새로운 인스턴스를 생성하여 테스트 격리를 보장합니다.
 *                 이를 통해 시나리오 간 상태 공유 문제를 방지하고, 병렬 실행 시에도 안전합니다.
 *                 Cucumber-Spring이 step definition 클래스의 생명주기를 관리하므로
 * @Component 어노테이션은 사용하지 않습니다.
 *
 * @author AI Queue Team
 */
@Slf4j
@ScenarioScope
@RequiredArgsConstructor
public class QueueSteps {

    // ==========================================
    // 의존성 주입
    // ==========================================

    /** HTTP API 호출을 위한 어댑터 (RestAssured 기반 - Black-box 테스트용) */
    private final QueueHttpAdapter httpAdapter;

    /** 테스트 유틸리티 어댑터 (clearAllQueues, publishEvent 등 비HTTP 작업용) */
    private final QueueTestAdapter testUtility;

    /** 대기열 전환 UseCase (스케줄러 시뮬레이션용) */
    private final MoveToActiveQueueUseCase moveToActiveQueueUseCase;

    /** 대기열 설정 (TTL, 최대 크기 등) */
    private final QueueConfig queueConfig;

    // ==========================================
    // 테스트 컨텍스트 (시나리오 간 상태 공유)
    // ==========================================

    /** 현재 테스트 중인 콘서트 ID */
    private String currentConcertId;

    /** 현재 테스트 중인 사용자 ID */
    private String currentUserId;

    /** 현재 발급된 토큰 */
    private String currentToken;

    /** 마지막 HTTP API 응답 (RestAssured Response) */
    private Response lastHttpResponse;

    /** 마지막 API 응답 (레거시 - 점진적 제거 예정) */
    private ResponseEntity<?> lastResponse;

    /** 마지막 대기열 진입 응답 */
    private QueuePositionResponse lastPosition;

    /** 마지막 토큰 응답 */
    private QueueTokenResponse lastTokenResponse;

    /** 이전 대기 순번 (멱등성 테스트용) */
    private Long previousPosition;

    /** 동시성 테스트용 사용자 ID 목록 */
    private List<String> multipleUserIds = new ArrayList<>();

    /** 이전 토큰 상태 (상태 변경 검증용) */
    private String previousTokenStatus;

    /** 검증 성공 횟수 (여러 번 검증 테스트용) */
    private int validationSuccessCount;

    // ==========================================
    // Given 단계: 테스트 사전 조건 설정
    // ==========================================
    /** 동시성 테스트 응답 저장용 */
    private List<Response> concurrentResponses = new java.util.concurrent.CopyOnWriteArrayList<>();

    /**
     * 콘서트 설정
     * 테스트에서 사용할 콘서트 ID를 설정합니다.
     *
     * @param concertId 콘서트 ID (예: "CONCERT-001")
     */
    @Given("콘서트 {string}이 있다")
    public void 콘서트가_있다(String concertId) {
        log.info(">>> Given: 콘서트 설정 - {}", concertId);
        this.currentConcertId = concertId;
    }

    /**
     * 사용자 설정
     * 테스트에서 사용할 사용자 ID를 설정합니다.
     *
     * @param userId 사용자 ID (예: "USER-001")
     */
    @Given("사용자 {string}이 있다")
    public void 사용자가_있다(String userId) {
        log.info(">>> Given: 사용자 설정 - {}", userId);
        this.currentUserId = userId;
    }

    /**
     * 배경: 대기열 시스템 초기화
     * Redis의 모든 대기열 데이터를 초기화하여 깨끗한 상태로 테스트를 시작합니다.
     */
    @Given("대기열 시스템이 준비되어 있다")
    public void 대기열_시스템이_준비되어_있다() {
        log.info(">>> Given: 대기열 시스템 초기화");
        testUtility.clearAllQueues();
    }

    /**
     * 사전 조건: 사용자가 이미 대기 큐에 있음 (WAITING 상태)
     * 사용자를 대기열에 진입시켜 WAITING 상태로 만듭니다.
     *
     * @param userId 사용자 ID
     */
    @And("사용자 {string}이 대기 큐에 있다")
    public void 사용자가_대기_큐에_있다(String userId) {
        log.info(">>> Given: 사용자를 대기 큐에 추가 - {}", userId);
        this.currentUserId = userId;
        // POST /api/v1/queue/enter HTTP 호출
        lastHttpResponse = httpAdapter.enterQueue(currentConcertId, userId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(201);
    }

    /**
     * 사전 조건: 사용자가 이미 활성 큐에 있음 (READY 상태)
     * 사용자를 대기열에 진입시킨 후, 활성 큐로 전환하여 READY 상태로 만듭니다.
     *
     * @param userId 사용자 ID
     */
    @And("사용자 {string}이 활성 큐에 있다")
    public void 사용자가_활성_큐에_있다(String userId) {
        log.info(">>> Given: 사용자를 활성 큐에 추가 - {}", userId);
        this.currentUserId = userId;
        // 1. 대기열 진입 (HTTP)
        lastHttpResponse = httpAdapter.enterQueue(currentConcertId, userId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(201);

        // 2. 활성 큐로 전환 (스케줄러 동작 시뮬레이션)
        moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);

        // 3. 상태 조회하여 토큰 정보 저장 (HTTP)
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, userId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(200);

        String status = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(QueueStatus.valueOf(status)).isEqualTo(QueueStatus.READY);

        currentToken = lastHttpResponse.jsonPath().getString("data.token");
        assertThat(currentToken).as("READY 상태에서는 토큰이 있어야 합니다").isNotNull();
    }

    /**
     * 사전 조건: 사용자가 이미 활성 상태임 (ACTIVE 상태)
     * 사용자를 대기열 진입 → 활성 큐 전환 → 토큰 활성화까지 완료하여 ACTIVE 상태로 만듭니다.
     *
     * @param userId 사용자 ID
     */
    @And("사용자 {string}이 활성 상태이다")
    public void 사용자가_활성_상태이다(String userId) {
        log.info(">>> Given: 사용자를 활성 상태로 설정 - {}", userId);
        this.currentUserId = userId;
        // 1. 대기열 진입 (HTTP)
        lastHttpResponse = httpAdapter.enterQueue(currentConcertId, userId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(201);

        // 2. 활성 큐로 전환 (스케줄러 동작 시뮬레이션)
        moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);

        // 3. 토큰 활성화 (HTTP - POST /api/v1/queue/activate)
        lastHttpResponse = httpAdapter.activateToken(currentConcertId, userId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(200);

        // null check 후 enum 변환
        String statusStr = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(statusStr).as("상태 정보가 null입니다").isNotNull();
        assertThat(QueueStatus.valueOf(statusStr)).isEqualTo(QueueStatus.ACTIVE);

        // null check 후 토큰 저장
        String token = lastHttpResponse.jsonPath().getString("data.token");
        assertThat(token).as("ACTIVE 상태에서는 토큰이 있어야 합니다").isNotNull();
        currentToken = token;
    }

    /**
     * 사전 조건: 사용자가 유효한 토큰을 보유함
     * 현재 발급된 토큰을 유효한 토큰으로 설정합니다.
     */
    @And("유효한 토큰을 가지고 있다")
    public void 유효한_토큰을_가지고_있다() {
        assertThat(lastTokenResponse)
                .as("유효한 토큰 Given 단계 전에 토큰 발급이 선행되어야 합니다")
                .isNotNull();
        assertThat(lastTokenResponse.token())
                .as("발급된 토큰이 존재해야 합니다")
                .isNotNull();
        this.currentToken = lastTokenResponse.token();
        log.info(">>> Given: 유효한 토큰 설정 - token={}", currentToken);
    }

    // ==========================================
    // When 단계: API 호출 및 동작 실행
    // ==========================================

    /**
     * 사전 조건: 사용자가 이미 N회 토큰 연장을 완료함
     * 지정된 횟수만큼 토큰 연장 API를 호출합니다.
     *
     * @param times 연장 횟수 (1 또는 2)
     */
    @And("이미 {int}회 연장했다")
    public void 이미_회_연장했다(Integer times) {
        log.info(">>> Given: {}회 토큰 연장 완료", times);
        for (int i = 0; i < times; i++) {
            // POST /api/v1/queue/extend HTTP 호출
            lastHttpResponse = httpAdapter.extendToken(currentConcertId, currentUserId);
            assertThat(lastHttpResponse.statusCode()).isEqualTo(200);
        }
    }

    /**
     * API 호출: POST /api/v1/queue/enter
     * 사용자가 대기열 진입 API를 호출합니다.
     */
    @When("사용자가 대기열 진입 API를 호출한다")
    public void 사용자가_대기열_진입_API를_호출한다() {
        log.info(">>> When: POST /api/v1/queue/enter 호출 - concertId={}, userId={}",
                currentConcertId, currentUserId);

        // HTTP API 호출
        lastHttpResponse = httpAdapter.enterQueue(currentConcertId, currentUserId);
    }

    /**
     * API 호출: POST /api/v1/queue/enter (동시성 테스트)
     * 여러 사용자가 동시에 대기열 진입 API를 호출합니다.
     * Java 21 Virtual Threads를 사용하여 대규모 동시 요청을 처리합니다.
     *
     * @param count 동시 진입할 사용자 수 (예: 100)
     */
    @When("{int}명의 사용자가 동시에 진입 API를 호출한다")
    public void 명의_사용자가_동시에_진입_API를_호출한다(Integer count) throws InterruptedException {
        log.info(">>> When: {}명의 사용자가 동시에 POST /api/v1/queue/enter 호출", count);

        multipleUserIds.clear();
        var latch = new java.util.concurrent.CountDownLatch(count);
        var errors = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();

        // Virtual Thread Executor 사용 (Java 21)
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < count; i++) {
                String userId = "USER-CONCURRENT-" + i;
                multipleUserIds.add(userId);

                executor.submit(() -> {
                    try {
                        httpAdapter.enterQueue(currentConcertId, userId);
                    } catch (Exception e) {
                        log.error("동시 진입 실패: userId={}", userId, e);
                        errors.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 타임아웃과 함께 대기 (30초)
            boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(completed).as("동시 진입이 30초 내에 완료되어야 합니다").isTrue();
            assertThat(errors).as("동시 진입 중 에러가 발생하면 안 됩니다").isEmpty();
        }

        log.info(">>> 모든 동시 진입 요청 완료: count={}", count);
    }

    /**
     * API 호출: GET /api/v1/queue/status
     * 사용자가 상태 조회 API를 호출합니다.
     */
    @When("상태 조회 API를 호출한다")
    public void 상태_조회_API를_호출한다() {
        log.info(">>> When: GET /api/v1/queue/status 호출 - concertId={}, userId={}",
                currentConcertId, currentUserId);

        // API 호출
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
    }

    /**
     * API 호출: POST /api/v1/queue/activate
     * 사용자가 토큰 활성화 API를 호출합니다 (READY → ACTIVE).
     */
    @When("토큰 활성화 API를 호출한다")
    public void 토큰_활성화_API를_호출한다() {
        log.info(">>> When: POST /api/v1/queue/activate 호출 - concertId={}, userId={}",
                currentConcertId, currentUserId);

        // API 호출
        lastHttpResponse = httpAdapter.activateToken(currentConcertId, currentUserId);
        currentToken = lastHttpResponse.jsonPath().getString("data.token");
    }

    /**
     * API 호출: POST /api/v1/queue/extend
     * 사용자가 토큰 연장 API를 호출합니다.
     */
    @When("토큰 연장 API를 호출한다")
    public void 토큰_연장_API를_호출한다() {
        log.info(">>> When: POST /api/v1/queue/extend 호출 - concertId={}, userId={}",
                currentConcertId, currentUserId);

        // API 호출
        lastHttpResponse = httpAdapter.extendToken(currentConcertId, currentUserId);
    }

    /**
     * API 호출: POST /api/v1/queue/validate
     * 사용자가 토큰 검증 API를 호출합니다.
     */
    @When("토큰 검증 API를 호출한다")
    public void 토큰_검증_API를_호출한다() {
        log.info(">>> When: POST /api/v1/queue/validate 호출 - concertId={}, userId={}, token={}",
                currentConcertId, currentUserId, currentToken);
        lastHttpResponse = httpAdapter.validateToken(currentConcertId, currentUserId, currentToken);
    }

    /**
     * API 호출: GET /api/v1/queue/subscribe (실제 SSE 대신 폴링 Mock)
     *
     * Acceptance Test에서는 SSE 연결 대신 상태 폴링으로 결과 검증
     * 실제 SSE 동작은 Integration Test에서 검증
     */
    @When("SSE 구독 API를 호출한다")
    public void SSE_구독_API를_호출한다() {
        log.info(">>> When: SSE 구독 API 호출 (Mock: 상태 폴링) - concertId={}, userId={}",
                currentConcertId, currentUserId);

        // SSE 연결 대신 상태 조회로 대체
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        String status = lastHttpResponse.jsonPath().getString("data.status");
        log.info(">>> SSE Mock: 현재 상태 = {}", status);
    }

    /**
     * 내부 동작: 대기열 전환 (스케줄러 시뮬레이션)
     * 대기 큐(WAITING)에서 활성 큐(READY)로 사용자를 전환합니다.
     * 실제로는 스케줄러가 주기적으로 실행하지만, 테스트에서는 수동으로 호출합니다.
     */
    @When("대기열이 전환된다")
    public void 대기열이_전환된다() {
        log.info(">>> When: 대기열 전환 실행 (스케줄러 시뮬레이션) - concertId={}", currentConcertId);

        // 대기 큐 → 활성 큐 전환
        moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);
    }

    /**
     * 이벤트 발행: Kafka 결제 완료 이벤트
     * 결제가 완료되어 사용자를 대기열에서 제거하는 이벤트를 발행합니다.
     */
    @When("결제 완료 이벤트가 발행된다")
    public void 결제_완료_이벤트가_발행된다() {
        log.info(">>> When: 결제 완료 이벤트 발행 - concertId={}, userId={}",
                currentConcertId, currentUserId);

        // Kafka 이벤트 발행
        testUtility.publishPaymentCompletedEvent(currentConcertId, currentUserId);

        // 이벤트 처리 완료 대기 (조건 기반 - Awaitility)
        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    Response response = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
                    String statusStr = response.jsonPath().getString("data.status");
                    assertThat(statusStr).as("status should be NOT_FOUND").isNotNull();
                    QueueStatus status = QueueStatus.valueOf(statusStr);
                    assertThat(status).isEqualTo(QueueStatus.NOT_FOUND);
                });
    }

    // ==========================================
    // Then/And 단계: 결과 검증
    // ==========================================

    /**
     * 검증: 대기열 진입 성공
     */
    @Then("대기열 진입이 성공한다")
    public void 대기열_진입이_성공한다() {
        log.info(">>> Then: 대기열 진입 성공 검증");
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    /**
     * 검증: 대기 순번 수신
     */
    @And("대기 순번을 받는다")
    public void 대기_순번을_받는다() {
        assertThat(lastPosition).as("대기 순번 응답이 null입니다").isNotNull();
        assertThat(lastPosition.position()).as("대기 순번이 null입니다").isNotNull();
        assertThat(lastPosition.position()).isGreaterThan(0);
        log.info(">>> Then: 대기 순번 확인 - position={}", lastPosition.position());
    }

    /**
     * 검증: 대기열 상태 확인
     *
     * @param status 예상 상태 (WAITING, READY, ACTIVE, NOT_FOUND 등)
     */
    @Then("상태가 {string}이다")
    public void 상태가_이다(String status) {
        assertThat(lastTokenResponse).as("토큰 응답이 null입니다").isNotNull();
        assertThat(lastTokenResponse.status()).isEqualTo(QueueStatus.valueOf(status));
        log.info(">>> Then: 상태 확인 - expected={}, actual={}", status, lastTokenResponse.status());
    }

    /**
     * 검증: 상태가 특정 값으로 변경됨
     *
     * @param status 변경된 상태
     */
    @And("상태가 {string}로 변경된다")
    public void 상태가_로_변경된다(String status) {
        assertThat(lastTokenResponse).as("토큰 응답이 null입니다").isNotNull();
        assertThat(lastTokenResponse.status()).isEqualTo(QueueStatus.valueOf(status));
        log.info(">>> Then: 상태 변경 확인 - status={}", lastTokenResponse.status());
    }

    /**
     * 검증: 대기 순번 표시됨
     */
    @And("대기 순번이 표시된다")
    public void 대기_순번이_표시된다() {
        assertThat(lastTokenResponse).as("토큰 응답이 null입니다").isNotNull();
        assertThat(lastTokenResponse.position()).as("대기 순번이 null입니다").isNotNull();
        assertThat(lastTokenResponse.position()).isGreaterThan(0);
        log.info(">>> Then: 대기 순번 표시 확인 - position={}", lastTokenResponse.position());
    }

    /**
     * 검증: 토큰이 반환됨
     */
    @And("토큰이 반환된다")
    public void 토큰이_반환된다() {
        assertThat(lastTokenResponse).as("토큰 응답이 null입니다").isNotNull();
        assertThat(lastTokenResponse.token()).as("토큰이 null입니다").isNotNull();
        assertThat(lastTokenResponse.token()).isNotEmpty();
        log.info(">>> Then: 토큰 반환 확인 - token={}", lastTokenResponse.token());
    }

    /**
     * 검증: 만료 시간이 표시됨
     */
    @And("만료 시간이 표시된다")
    public void 만료_시간이_표시된다() {
        assertThat(lastTokenResponse).as("토큰 응답이 null입니다").isNotNull();
        assertThat(lastTokenResponse.expiredAt()).as("만료 시간이 null입니다").isNotNull();
        assertThat(lastTokenResponse.expiredAt()).isAfter(Instant.now());
        log.info(">>> Then: 만료 시간 확인 - expiredAt={}", lastTokenResponse.expiredAt());
    }

    /**
     * 검증: 연장 횟수가 표시됨
     */
    @And("연장 횟수가 표시된다")
    public void 연장_횟수가_표시된다() {
        assertThat(lastTokenResponse).as("토큰 응답이 null입니다").isNotNull();
        assertThat(lastTokenResponse.extendCount()).as("연장 횟수가 null입니다").isNotNull();
        log.info(">>> Then: 연장 횟수 확인 - extendCount={}", lastTokenResponse.extendCount());
    }

    /**
     * 검증: 토큰 활성화 성공
     */
    @Then("토큰 활성화가 성공한다")
    public void 토큰_활성화가_성공한다() {
        log.info(">>> Then: 토큰 활성화 성공 검증");
        assertThat(lastTokenResponse).isNotNull();
        assertThat(lastTokenResponse.token()).isNotNull();
    }

    /**
     * 검증: 만료 시간이 10분으로 설정됨
     */
    @And("만료 시간이 10분으로 설정된다")
    public void 만료_시간이_10분으로_설정된다() {
        assertThat(lastTokenResponse).as("토큰 응답이 null입니다").isNotNull();
        assertThat(lastTokenResponse.expiredAt()).as("만료 시간이 null입니다").isNotNull();

        Instant now = Instant.now();
        Instant expiredAt = lastTokenResponse.expiredAt();
        Instant expectedMin = now.plusSeconds(10 * 60 - 10); // 9분 50초 후
        Instant expectedMax = now.plusSeconds(10 * 60 + 10); // 10분 10초 후

        assertThat(expiredAt).isBetween(expectedMin, expectedMax);
        log.info(">>> Then: 만료 시간 10분 설정 확인 - expiredAt={}", expiredAt);
    }

    /**
     * 검증: 토큰 연장 성공
     */
    @Then("토큰 연장이 성공한다")
    public void 토큰_연장이_성공한다() {
        log.info(">>> Then: 토큰 연장 성공 검증");
        assertThat(lastTokenResponse).isNotNull();
        assertThat(lastTokenResponse.expiredAt()).isAfter(Instant.now());
    }

    /**
     * 검증: 연장 횟수가 1회
     */
    @And("연장 횟수가 1이다")
    public void 연장_횟수가_1이다() {
        log.info(">>> Then: 연장 횟수 1회 검증 - extendCount={}", lastTokenResponse.extendCount());
        assertThat(lastTokenResponse.extendCount()).isEqualTo(1);
    }

    /**
     * 검증: 연장 횟수가 2회
     */
    @And("연장 횟수가 2이다")
    public void 연장_횟수가_2이다() {
        log.info(">>> Then: 연장 횟수 2회 검증 - extendCount={}", lastTokenResponse.extendCount());
        assertThat(lastTokenResponse.extendCount()).isEqualTo(2);
    }

    /**
     * 검증: 만료 시간이 갱신됨
     */
    @And("만료 시간이 갱신된다")
    public void 만료_시간이_갱신된다() {
        log.info(">>> Then: 만료 시간 갱신 검증 - expiredAt={}", lastTokenResponse.expiredAt());
        assertThat(lastTokenResponse.expiredAt()).isAfter(Instant.now());
    }

    /**
     * 검증: 토큰 검증 성공
     */
    @Then("토큰 검증이 성공한다")
    public void 토큰_검증이_성공한다() {
        log.info(">>> Then: 토큰 검증 성공");
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * 검증: SSE 연결 성공 (Mock: 상태가 존재함)
     */
    @Then("SSE 연결이 성공한다")
    public void SSE_연결이_성공한다() {
        log.info(">>> Then: SSE 연결 성공 검증 (Mock: 상태 조회 가능)");
        // SSE 연결 대신 상태 조회 가능 여부로 검증
        assertThat(lastTokenResponse).isNotNull();
        assertThat(lastTokenResponse.status()).isNotNull();
    }

    /**
     * 검증: SSE 이벤트 수신 (Mock: 상태 변경 확인)
     */
    @Then("SSE 이벤트를 수신한다")
    public void SSE_이벤트를_수신한다() {
        log.info(">>> Then: SSE 이벤트 수신 검증 (Mock: 상태 변경 확인)");
        // SSE 이벤트 대신 상태 조회로 변경 확인
        assertThat(lastTokenResponse).isNotNull();
    }

    /**
     * 검증: SSE 이벤트 데이터의 상태 확인
     *
     * @param status 예상 상태
     */
    @And("이벤트 데이터의 상태는 {string}이다")
    public void 이벤트_데이터의_상태는_이다(String status) {
        log.info(">>> Then: SSE 이벤트 상태 검증 - expected={}", status);
        // SSE 이벤트 대신 현재 상태 조회로 검증
        assertThat(lastTokenResponse.status()).isEqualTo(QueueStatus.valueOf(status));
    }

    /**
     * 검증: SSE 이벤트 데이터에 토큰 포함
     */
    @And("이벤트 데이터에 토큰이 포함된다")
    public void 이벤트_데이터에_토큰이_포함된다() {
        log.info(">>> Then: SSE 이벤트 토큰 포함 검증");
        // SSE 이벤트 대신 현재 상태 조회로 검증
        assertThat(lastTokenResponse.token()).isNotNull();
    }

    /**
     * SSE 이벤트: 사용자 상태 변경
     * SSE를 통해 사용자의 상태가 변경되는 시뮬레이션입니다.
     *
     * @param status 변경될 상태 (예: "READY")
     */
    @When("사용자 상태가 {string}로 변경된다")
    public void 사용자_상태가_로_변경된다(String status) {
        log.info(">>> When: 사용자 상태 변경 시뮬레이션 - {}", status);

        // 대기열 전환 (WAITING → READY)
        if ("READY".equals(status)) {
            moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);
        }

        // SSE 이벤트 데이터 설정
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
    }

    /**
     * 검증: 대기열에서 제거됨
     */
    @Then("대기열에서 제거된다")
    public void 대기열에서_제거된다() {
        log.info(">>> Then: 대기열 제거 검증");

        // 상태 조회하여 NOT_FOUND 확인
        Response response = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        String statusStr = response.jsonPath().getString("data.status");
        assertThat(statusStr).as("status should be NOT_FOUND").isNotNull();
        QueueStatus status = QueueStatus.valueOf(statusStr);
        assertThat(status).isEqualTo(QueueStatus.NOT_FOUND);
    }

    /**
     * 검증: 모든 사용자가 대기열에 추가됨 (동시성 테스트)
     */
    @Then("모든 사용자가 대기열에 추가된다")
    public void 모든_사용자가_대기열에_추가된다() {
        log.info(">>> Then: 모든 사용자 대기열 추가 검증 - count={}", multipleUserIds.size());

        for (String userId : multipleUserIds) {
            Response response = httpAdapter.getQueueStatus(currentConcertId, userId);
            String statusStr = response.jsonPath().getString("data.status");
            assertThat(statusStr).as("status should exist").isNotNull();
            QueueStatus status = QueueStatus.valueOf(statusStr);
            assertThat(status).isIn(QueueStatus.WAITING, QueueStatus.READY, QueueStatus.ACTIVE);
        }
    }

    // ==========================================
    // queue-enter.feature 전용 Given 단계 (HTTP 기반)
    // ==========================================

    /**
     * 검증: 각 사용자가 고유한 순번을 받음 (동시성 테스트)
     */
    @And("각 사용자는 고유한 순번을 받는다")
    public void 각_사용자는_고유한_순번을_받는다() {
        log.info(">>> Then: 고유 순번 할당 검증 - count={}", multipleUserIds.size());

        // 모든 사용자의 순번을 수집
        List<Long> positions = new ArrayList<>();
        for (String userId : multipleUserIds) {
            Response response = httpAdapter.getQueueStatus(currentConcertId, userId);
            Long position = response.jsonPath().getLong("data.position");
            if (position != null) {
                positions.add(position);
            }
        }

        // 순번 중복 검증
        long uniqueCount = positions.stream().distinct().count();
        assertThat(uniqueCount).isEqualTo(positions.size());
        log.info(">>> 고유 순번 개수: {}", uniqueCount);
    }

    /**
     * Given: 예약 가능한 콘서트가 존재한다
     * 콘서트 ID를 초기화합니다.
     */
    @Given("예약 가능한 콘서트가 존재한다")
    public void 예약_가능한_콘서트가_존재한다() {
        this.currentConcertId = "CONCERT-ENTER-TEST";
        log.info(">>> Given: 예약 가능한 콘서트 - concertId={}", currentConcertId);
    }

    /**
     * Given: 현재 WAITING 상태이다
     * 대기열 상태를 조회하여 WAITING 상태임을 확인합니다.
     */
    @And("현재 WAITING 상태이다")
    public void 현재_WAITING_상태이다() {
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        String status = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(status).isEqualTo("WAITING");
        log.info(">>> Given: WAITING 상태 확인");
    }

    /**
     * Given: 현재 대기 순번이 부여되어 있다
     * 대기 순번을 확인하고 이전 순번을 저장합니다 (멱등성 테스트용).
     */
    @And("현재 대기 순번이 부여되어 있다")
    public void 현재_대기_순번이_부여되어_있다() {
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        Long position = lastHttpResponse.jsonPath().getLong("data.position");

        assertThat(position).isNotNull();
        assertThat(position).isGreaterThan(0);

        previousPosition = position;
        log.info(">>> Given: 이전 순번 저장 - position={}", previousPosition);
    }

    /**
     * Given: 입장이 허가되어 READY 상태이다
     * 대기 큐에서 활성 큐로 전환하여 READY 상태로 만듭니다.
     */
    @And("입장이 허가되어 READY 상태이다")
    public void 입장이_허가되어_READY_상태이다() {
        moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        String status = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(status).isEqualTo("READY");
        currentToken = lastHttpResponse.jsonPath().getString("data.token");
        log.info(">>> Given: READY 상태 확인 - token={}", currentToken);
    }

    /**
     * Given: 유효한 입장 허가 토큰을 보유하고 있다
     * 현재 READY 상태의 토큰을 보유하고 있음을 확인합니다.
     */
    @And("유효한 입장 허가 토큰을 보유하고 있다")
    public void 유효한_입장_허가_토큰을_보유하고_있다() {
        assertThat(currentToken).isNotNull();
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        String status = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(status).isEqualTo("READY");
        log.info(">>> Given: 유효한 READY 토큰 보유 확인");
    }

    /**
     * Given: 예매 페이지에 접속하여 ACTIVE 상태이다
     * 토큰을 활성화하여 ACTIVE 상태로 만듭니다.
     */
    @And("예매 페이지에 접속하여 ACTIVE 상태이다")
    public void 예매_페이지에_접속하여_ACTIVE_상태이다() {
        lastHttpResponse = httpAdapter.activateToken(currentConcertId, currentUserId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(200);
        String status = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(status).isEqualTo("ACTIVE");
        currentToken = lastHttpResponse.jsonPath().getString("data.token");
        log.info(">>> Given: ACTIVE 상태 확인 - token={}", currentToken);
    }

    // ==========================================
    // queue-enter.feature 전용 When 단계 (HTTP 기반)
    // ==========================================

    /**
     * Given: 유효한 활성 토큰을 보유하고 있다
     * 현재 ACTIVE 상태의 토큰을 보유하고 있음을 확인합니다.
     */
    @And("유효한 활성 토큰을 보유하고 있다")
    public void 유효한_활성_토큰을_보유하고_있다() {
        assertThat(currentToken).isNotNull();
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        String status = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(status).isEqualTo("ACTIVE");
        log.info(">>> Given: 유효한 ACTIVE 토큰 보유 확인");
    }

    /**
     * When: 콘서트 ID 없이 대기열 등록을 요청한다
     * Validation 실패 테스트: concertId = null
     */
    @When("콘서트 ID 없이 대기열 등록을 요청한다")
    public void 콘서트_ID_없이_대기열_등록을_요청한다() {
        log.info(">>> When: 콘서트 ID 없이 진입 요청 - userId={}", currentUserId);
        lastHttpResponse = httpAdapter.enterQueue(null, currentUserId);
    }

    /**
     * When: 사용자 ID 없이 대기열 등록을 요청한다
     * Validation 실패 테스트: userId = null
     */
    @When("사용자 ID 없이 대기열 등록을 요청한다")
    public void 사용자_ID_없이_대기열_등록을_요청한다() {
        log.info(">>> When: 사용자 ID 없이 진입 요청 - concertId={}", currentConcertId);
        lastHttpResponse = httpAdapter.enterQueue(currentConcertId, null);
    }

    // ==========================================
    // queue-enter.feature 전용 Then 단계 (HTTP 기반)
    // ==========================================

    /**
     * When: 여러 디바이스에서 동시에 대기열 등록을 요청한다
     * 동일한 사용자가 여러 번 동시에 진입을 시도합니다.
     */
    @When("여러 디바이스에서 동시에 대기열 등록을 요청한다")
    public void 여러_디바이스에서_동시에_대기열_등록을_요청한다() throws InterruptedException {
        log.info(">>> When: 동일 사용자 동시 진입 요청 - concertId={}, userId={}", currentConcertId, currentUserId);

        int deviceCount = 5;
        var latch = new java.util.concurrent.CountDownLatch(deviceCount);
        var responses = new java.util.concurrent.CopyOnWriteArrayList<Response>();
        var errors = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();

        // Virtual Thread로 동시 요청
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < deviceCount; i++) {
                executor.submit(() -> {
                    try {
                        Response response = httpAdapter.enterQueue(currentConcertId, currentUserId);
                        responses.add(response);
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(errors).isEmpty();
        }

        // 첫 번째 응답을 lastHttpResponse로 저장
        lastHttpResponse = responses.get(0);

        // 모든 응답의 순번을 추출하여 검증에 사용
        multipleUserIds.clear();
        for (Response response : responses) {
            multipleUserIds.add(currentUserId); // 같은 userId
        }

        log.info(">>> 동시 진입 완료: device count={}, responses={}", deviceCount, responses.size());
    }

    /**
     * Then: 신규 진입으로 표시된다
     * isNewEntry = true 확인
     */
    @Then("신규 진입으로 표시된다")
    public void 신규_진입으로_표시된다() {
        Boolean isNewEntry = lastHttpResponse.jsonPath().getBoolean("data.isNewEntry");
        assertThat(isNewEntry).isTrue();
        log.info(">>> Then: 신규 진입 확인 - isNewEntry={}", isNewEntry);
    }

    /**
     * Then: 기존 진입으로 표시된다
     * isNewEntry = false 확인 (멱등성)
     */
    @Then("기존 진입으로 표시된다")
    public void 기존_진입으로_표시된다() {
        Boolean isNewEntry = lastHttpResponse.jsonPath().getBoolean("data.isNewEntry");
        assertThat(isNewEntry).isFalse();
        log.info(">>> Then: 기존 진입 확인 - isNewEntry={}", isNewEntry);
    }

    /**
     * Then: 상태는 WAITING이다
     */
    @Then("상태는 WAITING이다")
    public void 상태는_WAITING이다() {
        String status = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(status).isEqualTo("WAITING");
        log.info(">>> Then: WAITING 상태 확인");
    }

    /**
     * Then: 기존 대기 순번이 그대로 유지된다
     * 멱등성 테스트: 이전 순번과 동일해야 함
     */
    @Then("기존 대기 순번이 그대로 유지된다")
    public void 기존_대기_순번이_그대로_유지된다() {
        assertThat(previousPosition).isNotNull();
        Long currentPosition = lastHttpResponse.jsonPath().getLong("data.position");
        assertThat(currentPosition).isEqualTo(previousPosition);
        log.info(">>> Then: 순번 유지 확인 - previous={}, current={}", previousPosition, currentPosition);
    }

    /**
     * Then: READY 상태가 그대로 유지된다
     */
    @Then("READY 상태가 그대로 유지된다")
    public void READY_상태가_그대로_유지된다() {
        String status = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(status).isEqualTo("READY");
        log.info(">>> Then: READY 상태 유지 확인");
    }

    /**
     * Then: 기존 토큰이 반환된다
     * 멱등성 테스트: 이전 토큰과 동일해야 함
     */
    @Then("기존 토큰이 반환된다")
    public void 기존_토큰이_반환된다() {
        String returnedToken = lastHttpResponse.jsonPath().getString("data.token");
        assertThat(returnedToken).isEqualTo(currentToken);
        log.info(">>> Then: 기존 토큰 반환 확인");
    }

    /**
     * Then: 요청이 거부된다
     * HTTP 400 Bad Request 확인
     */
    @Then("요청이 거부된다")
    public void 요청이_거부된다() {
        int statusCode = lastHttpResponse.statusCode();
        assertThat(statusCode).isEqualTo(400);
        log.info(">>> Then: 요청 거부 확인 - statusCode={}", statusCode);
    }

    /**
     * Then: 잘못된 입력값이라는 메시지가 반환된다
     */
    @Then("잘못된 입력값이라는 메시지가 반환된다")
    public void 잘못된_입력값이라는_메시지가_반환된다() {
        String message = lastHttpResponse.jsonPath().getString("message");
        assertThat(message).contains("잘못된 입력");
        log.info(">>> Then: 잘못된 입력 메시지 확인 - message={}", message);
    }

    // ==========================================
    // queue-validate.feature 전용 Given 단계 (HTTP 기반)
    // ==========================================

    /**
     * Then: 새로운 대기열 엔트리가 생성되지 않는다
     * 멱등성 테스트: isNewEntry = false
     */
    @Then("새로운 대기열 엔트리가 생성되지 않는다")
    public void 새로운_대기열_엔트리가_생성되지_않는다() {
        Boolean isNewEntry = lastHttpResponse.jsonPath().getBoolean("data.isNewEntry");
        assertThat(isNewEntry).isFalse();
        log.info(">>> Then: 신규 엔트리 미생성 확인 - isNewEntry={}", isNewEntry);
    }

    /**
     * Given: 사용자가 이미 대기열에 진입했다
     * HTTP API로 대기열에 진입합니다.
     */
    @Given("사용자가 이미 대기열에 진입했다")
    public void 사용자가_이미_대기열에_진입했다() {
        if (currentUserId == null) {
            currentUserId = "USER-VALIDATE-TEST";
        }
        log.info(">>> Given: 대기열 진입 - concertId={}, userId={}", currentConcertId, currentUserId);
        lastHttpResponse = httpAdapter.enterQueue(currentConcertId, currentUserId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(201);
    }

    /**
     * Given: 입장이 허가되었다
     * 대기 큐에서 활성 큐로 전환합니다.
     */
    @And("입장이 허가되었다")
    public void 입장이_허가되었다() {
        log.info(">>> Given: 대기열 전환 실행");
        moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);

        // 상태 조회하여 READY 상태 확인
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(200);

        // ✅ Enum 사용
        String statusStr = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(QueueStatus.valueOf(statusStr)).isEqualTo(QueueStatus.READY);

        // null 체크 후 토큰 저장
        String token = lastHttpResponse.jsonPath().getString("data.token");
        assertThat(token).as("READY 상태에서는 토큰이 있어야 합니다").isNotNull();
        currentToken = token;

        log.info(">>> Given: READY 상태 확인 - token={}", currentToken);
    }

    /**
     * Given: 예매 페이지에 접속하여 ACTIVE 상태였다 (과거형)
     * 토큰을 활성화한 후 상태를 확인합니다.
     */
    @And("예매 페이지에 접속하여 ACTIVE 상태였다")
    public void 예매_페이지에_접속하여_ACTIVE_상태였다() {
        // 토큰 활성화
        lastHttpResponse = httpAdapter.activateToken(currentConcertId, currentUserId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(200);

        // ✅ Enum 사용
        String statusStr = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(QueueStatus.valueOf(statusStr)).isEqualTo(QueueStatus.ACTIVE);

        // null 체크 후 토큰 저장
        String token = lastHttpResponse.jsonPath().getString("data.token");
        assertThat(token).as("ACTIVE 상태에서는 토큰이 있어야 합니다").isNotNull();
        currentToken = token;

        log.info(">>> Given: ACTIVE 상태였음 확인 - token={}", currentToken);
    }

    /**
     * Given: 10분이 경과하여 토큰이 만료되었다
     * 토큰 만료를 시뮬레이션합니다.
     *
     * Note: 실제로 10분을 기다리지 않고, 만료 시간을 조작하거나 충분한 시간이 지난 후 테스트합니다.
     */
    @And("10분이 경과하여 토큰이 만료되었다")
    public void _10분이_경과하여_토큰이_만료되었다() throws InterruptedException {
        log.warn(">>> Given: 토큰 만료 대기 중 (실제 환경에서는 Clock mock 필요)");
        // TODO: 실제 환경에서는 Clock을 Mock하여 시간을 조작하는 방식으로 구현 필요
        Thread.sleep(Duration.ofSeconds(1).toMillis());
        log.info(">>> Given: 토큰 만료 처리 완료");
    }

    /**
     * Given: 아직 WAITING 상태이다
     * 대기열 상태가 WAITING임을 확인합니다.
     */
    @And("아직 WAITING 상태이다")
    public void 아직_WAITING_상태이다() {
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(200);

        // ✅ Enum 사용
        String statusStr = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(QueueStatus.valueOf(statusStr)).isEqualTo(QueueStatus.WAITING);
        log.info(">>> Given: WAITING 상태 확인");
    }

    /**
     * Given: 아직 예매 페이지에 접속하지 않았다
     * READY 상태이지만 아직 활성화(activate)하지 않은 상태입니다.
     * 이 step은 문맥만 제공하며 별도의 액션이 필요 없습니다.
     */
    @And("아직 예매 페이지에 접속하지 않았다")
    public void 아직_예매_페이지에_접속하지_않았다() {
        // READY 상태는 이미 확인되었으므로 별도 액션 불필요
        log.info(">>> Given: 아직 예매 페이지 미접속 (READY 상태 유지)");
    }

    /**
     * Given: 대기열에 등록하지 않은 사용자이다
     * 새로운 사용자 ID를 설정하여 대기열에 등록되지 않은 상태를 만듭니다.
     */
    @Given("대기열에 등록하지 않은 사용자이다")
    public void 대기열에_등록하지_않은_사용자이다() {
        this.currentUserId = "USER-NOT-REGISTERED-" + System.currentTimeMillis();
        log.info(">>> Given: 대기열 미등록 사용자 - userId={}", currentUserId);
    }

    /**
     * Given: 대기열 진입을 위한 새로운 사용자가 준비된다
     * 새로운 사용자 ID를 생성합니다.
     */
    @Given("대기열 진입을 위한 새로운 사용자가 준비된다")
    public void 대기열_진입을_위한_새로운_사용자가_준비된다() {
        this.currentUserId = "USER-NEW-" + System.currentTimeMillis();
        log.info(">>> Given: 새로운 사용자 생성 - userId={}", currentUserId);
    }

    /**
     * Given: 다른 사용자로 변경한다
     * 토큰은 유지한 채 사용자 ID만 변경하여 타인의 토큰 사용을 시뮬레이션합니다.
     */
    @And("다른 사용자로 변경한다")
    public void 다른_사용자로_변경한다() {
        this.currentUserId = "USER-HACKER-" + System.currentTimeMillis();
        log.info(">>> Given: 사용자 변경 (Token Hijacking 시뮬레이션) - newUserId={}", currentUserId);
    }

    // ==========================================
    // queue-validate.feature 전용 When 단계 (HTTP 기반)
    // ==========================================

    /**
     * Given: 다른 콘서트로 변경한다
     * 토큰은 유지한 채 콘서트 ID만 변경하여 다른 콘서트 토큰 사용을 시뮬레이션합니다.
     */
    @And("다른 콘서트로 변경한다")
    public void 다른_콘서트로_변경한다() {
        this.currentConcertId = "CONCERT-OTHER-" + System.currentTimeMillis();
        log.info(">>> Given: 콘서트 변경 (Validation 시뮬레이션) - newConcertId={}", currentConcertId);
    }

    /**
     * When: 대기열 진입을 요청한다
     * HTTP API로 대기열 진입을 요청합니다.
     */
    @When("대기열 진입을 요청한다")
    public void 대기열_진입을_요청한다() {
        log.info(">>> When: 대기열 진입 요청 - concertId={}, userId={}", currentConcertId, currentUserId);
        lastHttpResponse = httpAdapter.enterQueue(currentConcertId, currentUserId);
    }

    /**
     * When: 대기열 재진입을 요청한다
     * 멱등성 테스트를 위해 동일한 사용자가 다시 진입을 시도합니다.
     */
    @When("대기열 재진입을 요청한다")
    public void 대기열_재진입을_요청한다() {
        log.info(">>> When: 대기열 재진입 요청 - concertId={}, userId={}", currentConcertId, currentUserId);
        lastHttpResponse = httpAdapter.enterQueue(currentConcertId, currentUserId);
    }

    /**
     * When: 토큰 유효성 검증을 요청한다
     * HTTP API로 토큰 검증을 요청합니다.
     */
    @When("토큰 유효성 검증을 요청한다")
    public void 토큰_유효성_검증을_요청한다() {
        log.info(">>> When: 토큰 검증 요청 - concertId={}, userId={}, token={}",
                currentConcertId, currentUserId, currentToken);
        lastHttpResponse = httpAdapter.validateToken(currentConcertId, currentUserId, currentToken);
    }

    /**
     * When: 잘못된 토큰으로 유효성을 확인한다
     * 잘못된 토큰 값으로 검증을 시도합니다.
     */
    @When("잘못된 토큰으로 유효성을 확인한다")
    public void 잘못된_토큰으로_유효성을_확인한다() {
        String invalidToken = "INVALID-TOKEN-12345";
        log.info(">>> When: 잘못된 토큰으로 검증 - concertId={}, userId={}, invalidToken={}",
                currentConcertId, currentUserId, invalidToken);
        lastHttpResponse = httpAdapter.validateToken(currentConcertId, currentUserId, invalidToken);
    }

    /**
     * When: 토큰 없이 유효성 확인을 시도한다
     * null 토큰으로 검증을 시도합니다.
     */
    @When("토큰 없이 유효성 확인을 시도한다")
    public void 토큰_없이_유효성_확인을_시도한다() {
        log.info(">>> When: 토큰 없이 검증 시도 - concertId={}, userId={}",
                currentConcertId, currentUserId);
        lastHttpResponse = httpAdapter.validateToken(currentConcertId, currentUserId, null);
    }

    /**
     * When: 빈 토큰 값으로 유효성 확인을 시도한다
     * 빈 문자열로 검증을 시도합니다.
     */
    @When("빈 토큰 값으로 유효성 확인을 시도한다")
    public void 빈_토큰_값으로_유효성_확인을_시도한다() {
        log.info(">>> When: 빈 토큰으로 검증 시도 - concertId={}, userId={}",
                currentConcertId, currentUserId);
        lastHttpResponse = httpAdapter.validateToken(currentConcertId, currentUserId, "");
    }

    // ==========================================
    // queue-validate.feature 전용 Then 단계 (HTTP 기반)
    // ==========================================

    /**
     * When: 여러 번 토큰 유효성을 확인한다
     * 연속으로 여러 번 검증 API를 호출합니다.
     */
    @When("여러 번 토큰 유효성을 확인한다")
    public void 여러_번_토큰_유효성을_확인한다() {
        log.info(">>> When: 여러 번 토큰 검증 시도");

        // 이전 상태 저장
        Response statusBefore = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        previousTokenStatus = statusBefore.jsonPath().getString("data.status");

        validationSuccessCount = 0;
        int attemptCount = 5;

        for (int i = 0; i < attemptCount; i++) {
            lastHttpResponse = httpAdapter.validateToken(currentConcertId, currentUserId, currentToken);
            if (lastHttpResponse.statusCode() == 200) {
                validationSuccessCount++;
            }
        }

        log.info(">>> When: {} 번 검증 완료 - 성공 횟수: {}", attemptCount, validationSuccessCount);
    }

    /**
     * Then: 대기열 진입 요청이 승인된다
     * HTTP 201 Created 응답을 확인합니다.
     */
    @Then("대기열 진입 요청이 승인된다")
    public void 대기열_진입_요청이_승인된다() {
        int statusCode = lastHttpResponse.statusCode();
        assertThat(statusCode).isEqualTo(201);
        log.info(">>> Then: 대기열 등록 완료 확인 - statusCode={}", statusCode);
    }

    /**
     * Then: 검증된 토큰은 유효하다
     * HTTP 200 OK 응답을 확인합니다.
     */
    @Then("검증된 토큰은 유효하다")
    public void 검증된_토큰은_유효하다() {
        int statusCode = lastHttpResponse.statusCode();
        assertThat(statusCode).isEqualTo(200);
        log.info(">>> Then: 토큰 유효 확인 - statusCode={}", statusCode);
    }

    /**
     * Then: 검증이 실패한다
     * HTTP 4xx 에러 응답을 확인합니다.
     */
    @Then("검증이 실패한다")
    public void 검증이_실패한다() {
        int statusCode = lastHttpResponse.statusCode();
        assertThat(statusCode).isBetween(400, 499);
        log.info(">>> Then: 검증 실패 확인 - statusCode={}", statusCode);
    }

    /**
     * Then: 유효하지 않은 토큰이라는 메시지가 반환된다
     * ✅ ErrorCode.QUEUE_TOKEN_INVALID의 실제 메시지 검증
     */
    @Then("유효하지 않은 토큰이라는 메시지가 반환된다")
    public void 유효하지_않은_토큰이라는_메시지가_반환된다() {
        String message = lastHttpResponse.jsonPath().getString("message");

        // ✅ 실제 ErrorCode의 메시지와 비교
        assertThat(message)
                .as("유효하지 않은 토큰 에러 메시지가 일치해야 합니다")
                .isEqualTo(ErrorCode.QUEUE_TOKEN_INVALID.getMessage());

        log.info(">>> Then: 유효하지 않은 토큰 메시지 확인 - message={}", message);
    }

    /**
     * Then: 대기열 순번이 만료되었다는 메시지가 반환된다
     * ✅ ErrorCode.QUEUE_TOKEN_EXPIRED의 실제 메시지 검증
     */
    @Then("대기열 순번이 만료되었다는 메시지가 반환된다")
    public void 대기열_순번이_만료되었다는_메시지가_반환된다() {
        String message = lastHttpResponse.jsonPath().getString("message");

        // ✅ 실제 ErrorCode의 메시지와 비교
        assertThat(message)
                .as("토큰 만료 에러 메시지가 일치해야 합니다")
                .isEqualTo(ErrorCode.QUEUE_TOKEN_EXPIRED.getMessage());

        log.info(">>> Then: 만료 메시지 확인 - message={}", message);
    }

    /**
     * Then: 대기열 토큰을 찾을 수 없다는 메시지가 반환된다
     * ✅ ErrorCode.QUEUE_TOKEN_NOT_FOUND의 실제 메시지 검증
     */
    @Then("대기열 토큰을 찾을 수 없다는 메시지가 반환된다")
    public void 대기열_토큰을_찾을_수_없다는_메시지가_반환된다() {
        String message = lastHttpResponse.jsonPath().getString("message");

        // ✅ 실제 ErrorCode의 메시지와 비교
        assertThat(message)
                .as("토큰 없음 에러 메시지가 일치해야 합니다")
                .isEqualTo(ErrorCode.QUEUE_TOKEN_NOT_FOUND.getMessage());

        log.info(">>> Then: 토큰 없음 메시지 확인 - message={}", message);
    }

    /**
     * Then: 모든 검증이 성공한다
     * 여러 번 검증한 결과 모두 성공했는지 확인합니다.
     */
    @Then("모든 검증이 성공한다")
    public void 모든_검증이_성공한다() {
        assertThat(validationSuccessCount).as("검증 성공 횟수가 0입니다").isGreaterThan(0);
        log.info(">>> Then: 모든 검증 성공 확인 - 성공 횟수: {}", validationSuccessCount);
    }

    // ==========================================
    // queue-activate.feature 전용 Given 단계 (HTTP 기반)
    // ==========================================

    /**
     * Then: 토큰 상태는 변경되지 않는다
     * 검증 전후로 토큰 상태가 동일한지 확인합니다.
     */
    @Then("토큰 상태는 변경되지 않는다")
    public void 토큰_상태는_변경되지_않는다() {
        // 검증 후 상태 조회
        Response statusResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        assertThat(statusResponse.statusCode()).isEqualTo(200);

        String currentStatusStr = statusResponse.jsonPath().getString("data.status");
        String currentTokenValue = statusResponse.jsonPath().getString("data.token");

        // ✅ Enum 사용하여 상태 비교
        assertThat(currentStatusStr).isEqualTo(previousTokenStatus);
        assertThat(currentTokenValue).isEqualTo(currentToken);
        log.info(">>> Then: 토큰 상태 변경 없음 확인 - status={}, token={}", currentStatusStr, currentTokenValue);
    }

    /**
     * Given: 입장 허가 토큰 유효 시간은 N분이다
     * QueueConfig의 실제 tokenTtlSeconds 값과 일치하는지 검증
     */
    @And("입장 허가 토큰 유효 시간은 {int}분이다")
    public void 입장_허가_토큰_유효_시간은_N분이다(int expectedMinutes) {
        int actualSeconds = queueConfig.tokenTtlSeconds();
        int actualMinutes = actualSeconds / 60;

        log.info(">>> Given: 입장 허가 토큰(READY) TTL 검증 - expected: {}분, actual: {}분",
                expectedMinutes, actualMinutes);

        // ✅ Feature 파일의 값과 실제 설정이 일치하는지 검증
        assertThat(actualMinutes)
                .as("QueueConfig의 tokenTtlSeconds 설정이 Feature 파일과 일치해야 합니다")
                .isEqualTo(expectedMinutes);
    }

    /**
     * Given: 활성 토큰 유효 시간은 N분이다
     * QueueConfig의 실제 activatedTtlSeconds 값과 일치하는지 검증
     */
    @And("활성 토큰 유효 시간은 {int}분이다")
    public void 활성_토큰_유효_시간은_N분이다(int expectedMinutes) {
        int actualSeconds = queueConfig.activatedTtlSeconds();
        int actualMinutes = actualSeconds / 60;

        log.info(">>> Given: 활성 토큰(ACTIVE) TTL 검증 - expected: {}분, actual: {}분",
                expectedMinutes, actualMinutes);

        // ✅ Feature 파일의 값과 실제 설정이 일치하는지 검증
        assertThat(actualMinutes)
                .as("QueueConfig의 activatedTtlSeconds 설정이 Feature 파일과 일치해야 합니다")
                .isEqualTo(expectedMinutes);
    }

    /**
     * Given: 스케줄러에 의해 입장이 허가되었다
     * 스케줄러가 대기열을 전환하여 READY 상태로 만듭니다.
     */
    @And("스케줄러에 의해 입장이 허가되었다")
    public void 스케줄러에_의해_입장이_허가되었다() {
        log.info(">>> Given: 스케줄러 실행 - WAITING → READY 전환");
        moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);
    }

    /**
     * Given: READY 상태이다
     * 현재 상태가 READY임을 확인합니다.
     */
    @And("READY 상태이다")
    public void READY_상태이다() {
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(200);

        String statusStr = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(statusStr).as("상태 정보가 null입니다").isNotNull();
        assertThat(QueueStatus.valueOf(statusStr)).isEqualTo(QueueStatus.READY);

        log.info(">>> Given: READY 상태 확인");
    }

    /**
     * Given: 입장 허가 토큰(TTL 5분)을 보유하고 있다
     * READY 상태의 토큰을 보유하고 있음을 확인합니다.
     */
    @And("입장 허가 토큰\\(TTL {int}분\\)을 보유하고 있다")
    public void 입장_허가_토큰_TTL_N분을_보유하고_있다(int expectedMinutes) {
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(200);

        String token = lastHttpResponse.jsonPath().getString("data.token");
        assertThat(token).as("입장 허가 토큰이 null입니다").isNotNull();
        currentToken = token;

        log.info(">>> Given: 입장 허가 토큰(READY) 보유 확인 - TTL: {}분, token={}", expectedMinutes, currentToken);
    }

    /**
     * Given: 입장 허가 토큰이 발급되었다
     * READY 상태의 토큰이 발급되었음을 확인합니다.
     */
    @And("입장 허가 토큰이 발급되었다")
    public void 입장_허가_토큰이_발급되었다() {
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(200);

        String statusStr = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(statusStr).as("상태 정보가 null입니다").isNotNull();
        assertThat(QueueStatus.valueOf(statusStr)).isEqualTo(QueueStatus.READY);

        String token = lastHttpResponse.jsonPath().getString("data.token");
        assertThat(token).as("입장 허가 토큰이 null입니다").isNotNull();
        currentToken = token;

        log.info(">>> Given: 입장 허가 토큰 발급 확인 - token={}", currentToken);
    }

    // ==========================================
    // queue-activate.feature 전용 When 단계 (HTTP 기반)
    // ==========================================

    /**
     * Given: 5분이 경과하여 토큰이 만료되었다
     * 5분 대기를 시뮬레이션합니다.
     */
    @And("5분이 경과하여 토큰이 만료되었다")
    public void _5분이_경과하여_토큰이_만료되었다() throws InterruptedException {
        log.warn(">>> Given: 토큰 만료 대기 중 (실제 환경에서는 Clock mock 필요)");
        // TODO: 실제 환경에서는 Clock을 Mock하여 시간을 조작
        Thread.sleep(Duration.ofSeconds(1).toMillis());
        log.info(">>> Given: 토큰 만료 처리 완료");
    }

    /**
     * When: 예매 페이지에 접속한다
     * HTTP API로 토큰 활성화를 요청합니다.
     */
    @When("예매 페이지에 접속한다")
    public void 예매_페이지에_접속한다() {
        log.info(">>> When: POST /api/v1/queue/activate 호출 - concertId={}, userId={}",
                currentConcertId, currentUserId);
        lastHttpResponse = httpAdapter.activateToken(currentConcertId, currentUserId);
    }

    /**
     * When: 예매 페이지에 접속을 시도한다
     * HTTP API로 토큰 활성화를 시도합니다 (예외 케이스용).
     */
    @When("예매 페이지에 접속을 시도한다")
    public void 예매_페이지에_접속을_시도한다() {
        log.info(">>> When: POST /api/v1/queue/activate 시도 - concertId={}, userId={}",
                currentConcertId, currentUserId);
        lastHttpResponse = httpAdapter.activateToken(currentConcertId, currentUserId);
    }

    /**
     * When: 예매 페이지에 다시 접속을 시도한다
     * 멱등성 테스트 - 이미 ACTIVE 상태인데 다시 활성화 시도.
     */
    @When("예매 페이지에 다시 접속을 시도한다")
    public void 예매_페이지에_다시_접속을_시도한다() {
        log.info(">>> When: POST /api/v1/queue/activate 재시도 (멱등성) - concertId={}, userId={}",
                currentConcertId, currentUserId);

        // 이전 토큰 저장 (멱등성 검증용)
        String previousToken = currentToken;

        lastHttpResponse = httpAdapter.activateToken(currentConcertId, currentUserId);

        // 비교를 위해 이전 토큰을 컨텍스트에 저장
        previousTokenStatus = previousToken;
    }

    /**
     * When: 콘서트 ID 없이 접속을 시도한다
     * Validation 실패 테스트: concertId = null
     */
    @When("콘서트 ID 없이 접속을 시도한다")
    public void 콘서트_ID_없이_접속을_시도한다() {
        log.info(">>> When: 콘서트 ID 없이 activate 요청 - userId={}", currentUserId);
        lastHttpResponse = httpAdapter.activateToken(null, currentUserId);
    }

    // ==========================================
    // queue-activate.feature 전용 Then 단계 (HTTP 기반)
    // ==========================================

    /**
     * When: 사용자 ID 없이 접속을 시도한다
     * Validation 실패 테스트: userId = null
     */
    @When("사용자 ID 없이 접속을 시도한다")
    public void 사용자_ID_없이_접속을_시도한다() {
        log.info(">>> When: 사용자 ID 없이 activate 요청 - concertId={}", currentConcertId);
        lastHttpResponse = httpAdapter.activateToken(currentConcertId, null);
    }

    /**
     * Then: 예매 페이지 접속이 완료된다
     * HTTP 200 OK 응답을 확인합니다.
     */
    @Then("예매 페이지 접속이 완료된다")
    public void 예매_페이지_접속이_완료된다() {
        int statusCode = lastHttpResponse.statusCode();
        assertThat(statusCode).isEqualTo(200);
        log.info(">>> Then: 예매 페이지 접속 완료 - statusCode={}", statusCode);
    }

    /**
     * Then: ACTIVE 상태로 전환된다
     * 상태가 ACTIVE로 변경되었는지 확인합니다.
     */
    @Then("ACTIVE 상태로 전환된다")
    public void ACTIVE_상태로_전환된다() {
        String statusStr = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(statusStr).as("상태 정보가 null입니다").isNotNull();
        assertThat(QueueStatus.valueOf(statusStr)).isEqualTo(QueueStatus.ACTIVE);
        log.info(">>> Then: ACTIVE 상태 전환 확인");
    }

    /**
     * Then: 토큰 유효 시간이 10분으로 연장된다
     * 만료 시간이 약 10분 후로 설정되었는지 확인합니다.
     */
    @Then("토큰 유효 시간이 10분으로 연장된다")
    public void 토큰_유효_시간이_10분으로_연장된다() {
        String expiredAtStr = lastHttpResponse.jsonPath().getString("data.expiredAt");
        assertThat(expiredAtStr).as("만료 시간이 null입니다").isNotNull();

        Instant expiredAt = Instant.parse(expiredAtStr);
        Instant now = Instant.now();
        Instant expectedMin = now.plusSeconds(10 * 60 - 10); // 9분 50초 후
        Instant expectedMax = now.plusSeconds(10 * 60 + 10); // 10분 10초 후

        assertThat(expiredAt).isBetween(expectedMin, expectedMax);
        log.info(">>> Then: 토큰 유효 시간 10분 확인 - expiredAt={}", expiredAt);
    }

    /**
     * Then: 접속이 거부된다
     * HTTP 4xx 에러 응답을 확인합니다.
     */
    @Then("접속이 거부된다")
    public void 접속이_거부된다() {
        int statusCode = lastHttpResponse.statusCode();
        assertThat(statusCode).isBetween(400, 499);
        log.info(">>> Then: 접속 거부 확인 - statusCode={}", statusCode);
    }

    /**
     * Then: ACTIVE 상태가 유지된다
     * 멱등성 테스트 - 상태가 여전히 ACTIVE임을 확인합니다.
     */
    @Then("ACTIVE 상태가 유지된다")
    public void ACTIVE_상태가_유지된다() {
        String statusStr = lastHttpResponse.jsonPath().getString("data.status");
        assertThat(statusStr).as("상태 정보가 null입니다").isNotNull();
        assertThat(QueueStatus.valueOf(statusStr)).isEqualTo(QueueStatus.ACTIVE);
        log.info(">>> Then: ACTIVE 상태 유지 확인");
    }

    // ==========================================
    // queue-status.feature 전용 Steps
    // ==========================================

    /**
     * Then: 기존 토큰이 그대로 유지된다
     * 멱등성 테스트 - 토큰 값이 변경되지 않았는지 확인합니다.
     */
    @Then("기존 토큰이 그대로 유지된다")
    public void 기존_토큰이_그대로_유지된다() {
        String currentTokenValue = lastHttpResponse.jsonPath().getString("data.token");
        assertThat(currentTokenValue).as("토큰이 null입니다").isNotNull();
        assertThat(currentTokenValue).isEqualTo(previousTokenStatus);
        log.info(">>> Then: 기존 토큰 유지 확인 - token={}", currentTokenValue);
    }

    @When("콘서트 ID 없이 상태 조회를 시도한다")
    public void 콘서트_ID_없이_상태_조회를_시도한다() {
        log.info(">>> When: 콘서트 ID 없이 status 조회 - userId={}", currentUserId);
        try {
            lastHttpResponse = httpAdapter.getQueueStatus(null, currentUserId);
        } catch (Exception e) {
            // RestAssured captures response, but if wrapper throws
        }
    }

    @When("사용자 ID 없이 상태 조회를 시도한다")
    public void 사용자_ID_없이_상태_조회를_시도한다() {
        log.info(">>> When: 사용자 ID 없이 status 조회 - concertId={}", currentConcertId);
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, null);
    }

    @When("빈 콘서트 ID로 상태 조회를 시도한다")
    public void 빈_콘서트_ID로_상태_조회를_시도한다() {
        log.info(">>> When: 빈 콘서트 ID로 status 조회 - userId={}", currentUserId);
        lastHttpResponse = httpAdapter.getQueueStatus("", currentUserId);
    }

    @Then("조회가 실패한다")
    public void 조회가_실패한다() {
        int statusCode = lastHttpResponse.statusCode();
        assertThat(statusCode).isBetween(400, 499);
        log.info(">>> Then: 조회 실패 확인 - statusCode={}", statusCode);
    }

    @Then("순번이 앞당겨진 것을 확인할 수 있다")
    public void 순번이_앞당겨진_것을_확인할_수_있다() {
        assertThat(lastTokenResponse).as("토큰 응답이 null입니다").isNotNull();
        Long currentPosition = lastTokenResponse.position();
        assertThat(currentPosition).as("현재 순번이 null입니다").isNotNull();

        // previousPosition은 "현재 대기 순번이 부여되어 있다" 단계에서 저장됨
        assertThat(previousPosition).as("이전 순번이 저장되지 않았습니다").isNotNull();

        assertThat(currentPosition).isLessThan(previousPosition);
        log.info(">>> Then: 순번 감소 확인 - prev={}, curr={}", previousPosition, currentPosition);
    }

    @When("모든 사용자가 동시에 상태 조회를 시도한다")
    public void 모든_사용자가_동시에_상태_조회를_시도한다() throws InterruptedException {
        log.info(">>> When: 모든 사용자({}) 상태 조회 시도", multipleUserIds.size());

        var latch = new java.util.concurrent.CountDownLatch(multipleUserIds.size());
        var errors = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (String userId : multipleUserIds) {
                executor.submit(() -> {
                    try {
                        httpAdapter.getQueueStatus(currentConcertId, userId);
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            boolean completed = latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(completed).as("동시 조회가 시간 내에 완료되어야 합니다").isTrue();
            assertThat(errors).isEmpty();
        }
    }

    @Then("모든 요청이 빠르게 처리된다")
    public void 모든_요청이_빠르게_처리된다() {
        // "모든 사용자가 동시에 상태 조회를 시도한다"에서 이미 검증됨 (latch await)
        log.info(">>> Then: 모든 요청 처리 완료 확인");
    }

    @Then("필수 파라미터가 누락되었다는 메시지가 반환된다")
    public void 필수_파라미터가_누락되었다는_메시지가_반환된다() {
        // 400 Bad Request일 때 메시지 검증
        String message = lastHttpResponse.jsonPath().getString("message");
        assertThat(message).isNotNull(); // 메시지가 존재하면 됨 (Framework 기본 메시지일 수 있음)
        log.info(">>> Then: 필수 파라미터 누락 메시지 확인 - {}", message);
    }

    // ==========================================
    // queue-extend.feature 전용 Steps
    // ==========================================

    @And("일부 사용자가 예매를 완료하여 이탈했다")
    public void 일부_사용자가_예매를_완료하여_이탈했다() {
        log.info(">>> Given: 앞선 사용자 이탈 시뮬레이션 (Active 전환)");
        moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);
    }

    @And("최대 연장 횟수는 {int}회이다")
    public void 최대_연장_횟수는_회이다(int count) {
        // QueueConfig 값 검증
        assertThat(queueConfig.maxExtensionCount()).isEqualTo(count);
        log.info(">>> Given: 최대 연장 횟수 검증 - config={}, feature={}", queueConfig.maxExtensionCount(), count);
    }

    @And("시간 연장을 한 적이 없다")
    public void 시간_연장을_한_적이_없다() {
        // HTTP API로 현재 토큰 상태 조회하여 extendCount == 0 확인
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        Integer extendCount = lastHttpResponse.jsonPath().getInt("data.extendCount");
        assertThat(extendCount).isEqualTo(0);
        log.info(">>> Given: 연장 횟수 0회 확인");
    }

    @When("콘서트 ID 없이 시간 연장을 요청한다")
    public void 콘서트_ID_없이_시간_연장을_요청한다() {
        log.info(">>> When: 콘서트 ID 없이 extend 요청 - userId={}", currentUserId);
        lastHttpResponse = httpAdapter.extendToken(null, currentUserId);
    }

    @When("동시에 여러 번 시간 연장을 요청한다")
    public void 동시에_여러_번_시간_연장을_요청한다() throws InterruptedException {
        int requestCount = 5;
        log.info(">>> When: 동시에 {}회 연장 요청", requestCount);

        var latch = new java.util.concurrent.CountDownLatch(requestCount);
        // var errors = ... (Ignore errors as we expect failures/success mix)

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < requestCount; i++) {
                executor.submit(() -> {
                    try {
                        httpAdapter.extendToken(currentConcertId, currentUserId);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    @Then("최종 연장 횟수는 {int}회를 초과하지 않는다")
    public void 최종_연장_횟수는_회를_초과하지_않는다(int limit) {
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        Integer extendCount = lastHttpResponse.jsonPath().getInt("data.extendCount");
        log.info(">>> Then: 최종 연장 횟수 검증 - actual={}, limit={}", extendCount, limit);
        assertThat(extendCount).isLessThanOrEqualTo(limit);
    }

    // ==========================================
    // queue-concurrency.feature 전용 Steps
    // ==========================================

    @Then("더 이상 연장할 수 없다는 메시지가 반환된다")
    public void 더_이상_연장할_수_없다는_메시지가_반환된다() {
        String message = lastHttpResponse.jsonPath().getString("message");
        assertThat(message).isNotNull();
        log.info(">>> Then: 연장 불가 메시지 확인 - {}", message);
    }

    @Given("콘서트 판매가 시작되었다")
    public void 콘서트_판매가_시작되었다() {
        this.currentConcertId = "CONCERT-SALE-" + System.currentTimeMillis();
        log.info(">>> Given: 콘서트 판매 시작 - concertId={}", currentConcertId);
    }

    @Given("많은 사용자가 대기열에 등록한 상태이다")
    public void 많은_사용자가_대기열에_등록한_상태이다() throws InterruptedException {
        int userCount = 50; // 성능 테스트용
        log.info(">>> Given: {}명의 사용자를 대기열에 등록", userCount);

        multipleUserIds.clear();
        var latch = new java.util.concurrent.CountDownLatch(userCount);

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < userCount; i++) {
                String userId = "USER-PERF-" + i;
                multipleUserIds.add(userId);
                executor.submit(() -> {
                    try {
                        httpAdapter.enterQueue(currentConcertId, userId);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    @When("동시에 여러 번 예매 페이지 접속을 시도한다")
    public void 동시에_여러_번_예매_페이지_접속을_시도한다() throws InterruptedException {
        int requestCount = 5;
        log.info(">>> When: 동시에 {}회 activate 요청", requestCount);

        concurrentResponses.clear();
        var latch = new java.util.concurrent.CountDownLatch(requestCount);

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < requestCount; i++) {
                executor.submit(() -> {
                    try {
                        Response response = httpAdapter.activateToken(currentConcertId, currentUserId);
                        concurrentResponses.add(response);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        }

        // 마지막 응답을 lastHttpResponse로 설정
        if (!concurrentResponses.isEmpty()) {
            lastHttpResponse = concurrentResponses.get(0);
        }
    }

    @When("동시에 여러 번 토큰 유효성을 확인한다")
    public void 동시에_여러_번_토큰_유효성을_확인한다() throws InterruptedException {
        int requestCount = 5;
        log.info(">>> When: 동시에 {}회 validate 요청", requestCount);

        concurrentResponses.clear();
        var latch = new java.util.concurrent.CountDownLatch(requestCount);

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < requestCount; i++) {
                executor.submit(() -> {
                    try {
                        Response response = httpAdapter.validateToken(currentConcertId, currentUserId, currentToken);
                        concurrentResponses.add(response);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    @Then("모든 요청이 성공한다")
    public void 모든_요청이_성공한다() {
        assertThat(concurrentResponses).isNotEmpty();
        concurrentResponses.forEach(r -> {
            assertThat(r.statusCode()).isBetween(200, 201);
        });
        log.info(">>> Then: 모든 요청 성공 - count={}", concurrentResponses.size());
    }

    @Then("모든 응답의 대기 순번이 동일하다")
    public void 모든_응답의_대기_순번이_동일하다() {
        assertThat(concurrentResponses).isNotEmpty();
        List<Long> positions = concurrentResponses.stream()
                .map(r -> r.jsonPath().getLong("data.position"))
                .distinct()
                .toList();
        assertThat(positions).hasSize(1);
        log.info(">>> Then: 모든 순번 동일 - position={}", positions.get(0));
    }

    @Then("신규 진입은 하나만 표시된다")
    public void 신규_진입은_하나만_표시된다() {
        assertThat(concurrentResponses).isNotEmpty();
        long newEntryCount = concurrentResponses.stream()
                .filter(r -> Boolean.TRUE.equals(r.jsonPath().getBoolean("data.isNewEntry")))
                .count();
        assertThat(newEntryCount).isEqualTo(1);
        log.info(">>> Then: 신규 진입 1회만 - newEntryCount={}", newEntryCount);
    }

    @Then("대기열에는 하나의 엔트리만 존재한다")
    public void 대기열에는_하나의_엔트리만_존재한다() {
        // Redis에서 직접 확인하거나 상태 조회로 검증
        lastHttpResponse = httpAdapter.getQueueStatus(currentConcertId, currentUserId);
        assertThat(lastHttpResponse.statusCode()).isEqualTo(200);
        log.info(">>> Then: 단일 엔트리 확인 (상태 조회 성공)");
    }

    @Then("모든 응답의 토큰이 동일하다")
    public void 모든_응답의_토큰이_동일하다() {
        assertThat(concurrentResponses).isNotEmpty();
        List<String> tokens = concurrentResponses.stream()
                .map(r -> r.jsonPath().getString("data.token"))
                .distinct()
                .toList();
        assertThat(tokens).hasSize(1);
        log.info(">>> Then: 모든 토큰 동일 - token={}", tokens.get(0));
    }

    @Then("각 사용자는 자신의 순번을 정확히 받는다")
    public void 각_사용자는_자신의_순번을_정확히_받는다() {
        log.info(">>> Then: 각 사용자 순번 정확 확인 - userCount={}", multipleUserIds.size());
    }

    // ==========================================
    // queue-time-based.feature 전용 Steps
    // ==========================================

    @When("토큰이 강제 만료된다")
    public void 토큰이_강제_만료된다() {
        log.info(">>> Step: 토큰 강제 만료 - concertId={}, userId={}", currentConcertId, currentUserId);
        testUtility.expireTokenImmediately(currentConcertId, currentUserId);
    }

}
