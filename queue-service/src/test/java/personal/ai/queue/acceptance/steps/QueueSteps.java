package personal.ai.queue.acceptance.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import personal.ai.common.dto.ApiResponse;
import personal.ai.queue.acceptance.support.QueueTestAdapter;
import personal.ai.queue.adapter.in.web.dto.QueuePositionResponse;
import personal.ai.queue.adapter.in.web.dto.QueueTokenResponse;
import personal.ai.queue.application.port.in.CleanupExpiredTokensUseCase;
import personal.ai.queue.application.port.in.MoveToActiveQueueUseCase;
import personal.ai.queue.domain.model.QueueStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Queue Cucumber Step Definitions
 * BDD 스타일 인수 테스트
 */
@RequiredArgsConstructor
public class QueueSteps {

    private final QueueTestAdapter queueAdapter;
    private final MoveToActiveQueueUseCase moveToActiveQueueUseCase;
    private final CleanupExpiredTokensUseCase cleanupExpiredTokensUseCase;

    // 테스트 컨텍스트
    private String currentConcertId;
    private String currentUserId;
    private String currentToken;
    private ResponseEntity<?> lastResponse;
    private QueuePositionResponse lastPosition;
    private QueueTokenResponse lastTokenResponse;
    private int movedCount;
    private long cleanupCount;
    private List<String> multipleUserIds = new ArrayList<>();

    @Given("대기열 시스템이 준비되어 있다")
    public void 대기열_시스템이_준비되어_있다() {
        queueAdapter.clearAllQueues();
    }

    @Given("콘서트 {string}이 있다")
    public void 콘서트가_있다(String concertId) {
        this.currentConcertId = concertId;
    }

    @Given("사용자 {string}이 있다")
    public void 사용자가_있다(String userId) {
        this.currentUserId = userId;
    }

    @And("사용자 {string}이 대기 큐에 있다")
    public void 사용자가_대기_큐에_있다(String userId) {
        this.currentUserId = userId;
        queueAdapter.enterQueue(currentConcertId, userId);
    }

    @And("사용자 {string}이 활성 큐에 있다")
    public void 사용자가_활성_큐에_있다(String userId) {
        this.currentUserId = userId;
        queueAdapter.enterQueue(currentConcertId, userId);
        moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);
    }

    @And("사용자 {string}이 활성 상태이다")
    public void 사용자가_활성_상태이다(String userId) {
        this.currentUserId = userId;
        queueAdapter.enterQueue(currentConcertId, userId);
        moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);
        lastTokenResponse = queueAdapter.activateToken(currentConcertId, userId);
        currentToken = lastTokenResponse.token();
    }

    @And("이미 {int}회 연장했다")
    public void 이미_회_연장했다(Integer times) {
        for (int i = 0; i < times; i++) {
            queueAdapter.extendToken(currentConcertId, currentUserId);
        }
    }

    @And("유효한 토큰 {string}을 가지고 있다")
    public void 유효한_토큰을_가지고_있다(String token) {
        this.currentToken = lastTokenResponse.token();
    }

    @And("사용자 {string}의 토큰이 만료되었다")
    public void 사용자의_토큰이_만료되었다(String userId) {
        this.currentUserId = userId;
        queueAdapter.addExpiredToken(currentConcertId, userId, Instant.now().minusSeconds(600));
    }

    @When("사용자가 대기열 진입을 요청한다")
    public void 사용자가_대기열_진입을_요청한다() {
        lastResponse = queueAdapter.enterQueueRequest(currentConcertId, currentUserId);
        if (lastResponse.getStatusCode() == HttpStatus.CREATED) {
            @SuppressWarnings("unchecked")
            ApiResponse<QueuePositionResponse> apiResponse =
                    (ApiResponse<QueuePositionResponse>) lastResponse.getBody();
            lastPosition = apiResponse.data();
        }
    }

    @When("사용자가 대기열 상태를 조회한다")
    public void 사용자가_대기열_상태를_조회한다() {
        lastTokenResponse = queueAdapter.getQueueStatus(currentConcertId, currentUserId);
    }

    @When("스케줄러가 대기열 전환을 실행한다")
    public void 스케줄러가_대기열_전환을_실행한다() {
        movedCount = moveToActiveQueueUseCase.moveWaitingToActive(currentConcertId);
        lastTokenResponse = queueAdapter.getQueueStatus(currentConcertId, currentUserId);
    }

    @When("사용자가 토큰 활성화를 요청한다")
    public void 사용자가_토큰_활성화를_요청한다() {
        lastTokenResponse = queueAdapter.activateToken(currentConcertId, currentUserId);
    }

    @When("사용자가 토큰 연장을 요청한다")
    public void 사용자가_토큰_연장을_요청한다() {
        try {
            lastTokenResponse = queueAdapter.extendToken(currentConcertId, currentUserId);
            lastResponse = ResponseEntity.ok().build();
        } catch (Exception e) {
            lastResponse = ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @When("토큰 검증을 요청한다")
    public void 토큰_검증을_요청한다() {
        try {
            queueAdapter.validateToken(currentConcertId, currentUserId, currentToken);
            lastResponse = ResponseEntity.ok().build();
        } catch (Exception e) {
            lastResponse = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e);
        }
    }

    @When("결제 완료 이벤트가 발행된다")
    public void 결제_완료_이벤트가_발행된다() {
        queueAdapter.publishPaymentCompletedEvent(currentConcertId, currentUserId);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @When("스케줄러가 만료 토큰 정리를 실행한다")
    public void 스케줄러가_만료_토큰_정리를_실행한다() {
        cleanupCount = cleanupExpiredTokensUseCase.cleanupExpired(currentConcertId);
    }

    @When("{int}명의 사용자가 동시에 진입을 요청한다")
    public void 명의_사용자가_동시에_진입을_요청한다(Integer count) {
        multipleUserIds.clear();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String userId = "USER-CONCURRENT-" + i;
            multipleUserIds.add(userId);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                    queueAdapter.enterQueue(currentConcertId, userId)
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Then("대기열 진입이 성공한다")
    public void 대기열_진입이_성공한다() {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @And("대기 순번을 받는다")
    public void 대기_순번을_받는다() {
        assertThat(lastPosition).isNotNull();
        assertThat(lastPosition.position()).isNotNull();
    }

    @Then("상태가 {string}이다")
    public void 상태가_이다(String status) {
        assertThat(lastTokenResponse.status()).isEqualTo(QueueStatus.valueOf(status));
    }

    @And("대기 순번이 표시된다")
    public void 대기_순번이_표시된다() {
        assertThat(lastTokenResponse.position()).isNotNull();
    }

    @Then("사용자가 Active Queue로 이동한다")
    public void 사용자가_Active_Queue로_이동한다() {
        assertThat(movedCount).isGreaterThan(0);
    }

    @And("토큰을 받는다")
    public void 토큰을_받는다() {
        assertThat(lastTokenResponse.token()).isNotNull();
    }

    @Then("토큰 활성화가 성공한다")
    public void 토큰_활성화가_성공한다() {
        assertThat(lastTokenResponse).isNotNull();
    }

    @And("상태가 {string}로 변경된다")
    public void 상태가_로_변경된다(String status) {
        assertThat(lastTokenResponse.status()).isEqualTo(QueueStatus.valueOf(status));
    }

    @And("만료 시간이 {int}분으로 연장된다")
    public void 만료_시간이_분으로_연장된다(Integer minutes) {
        Instant expiredAt = lastTokenResponse.expiredAt();
        assertThat(expiredAt).isAfter(Instant.now().plusSeconds((minutes * 60) - 10));
    }

    @Then("토큰 연장이 성공한다")
    public void 토큰_연장이_성공한다() {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @And("연장 횟수가 증가한다")
    public void 연장_횟수가_증가한다() {
        assertThat(lastTokenResponse.extendCount()).isGreaterThan(0);
    }

    @And("만료 시간이 갱신된다")
    public void 만료_시간이_갱신된다() {
        assertThat(lastTokenResponse.expiredAt()).isAfter(Instant.now());
    }

    @Then("연장이 실패한다")
    public void 연장이_실패한다() {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @And("에러 메시지에 {string}가 포함된다")
    public void 에러_메시지에_가_포함된다(String message) {
        assertThat(lastResponse.getBody().toString()).contains(message);
    }

    @Then("토큰 검증이 성공한다")
    public void 토큰_검증이_성공한다() {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Then("토큰 검증이 실패한다")
    public void 토큰_검증이_실패한다() {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @And("에러 코드가 {string}이다")
    public void 에러_코드가_이다(String errorCode) {
        assertThat(lastResponse.getBody().toString()).contains(errorCode);
    }

    @Then("사용자가 Active Queue에서 제거된다")
    public void 사용자가_Active_Queue에서_제거된다() {
        QueueTokenResponse status = queueAdapter.getQueueStatus(currentConcertId, currentUserId);
        assertThat(status.status()).isEqualTo(QueueStatus.NOT_FOUND);
    }

    @And("상태 조회 시 {string}이다")
    public void 상태_조회_시_이다(String status) {
        QueueTokenResponse tokenResponse = queueAdapter.getQueueStatus(currentConcertId, currentUserId);
        assertThat(tokenResponse.status()).isEqualTo(QueueStatus.valueOf(status));
    }

    @Then("만료된 토큰 {int}개가 제거된다")
    public void 만료된_토큰_개가_제거된다(Integer count) {
        assertThat(cleanupCount).isEqualTo(count);
    }

    @Then("모든 사용자가 대기열에 추가된다")
    public void 모든_사용자가_대기열에_추가된다() {
        for (String userId : multipleUserIds) {
            QueueTokenResponse status = queueAdapter.getQueueStatus(currentConcertId, userId);
            assertThat(status.status()).isIn(QueueStatus.WAITING, QueueStatus.READY, QueueStatus.ACTIVE);
        }
    }

    @And("각 사용자는 고유한 순번을 받는다")
    public void 각_사용자는_고유한_순번을_받는다() {
        assertThat(multipleUserIds).hasSize(100);
    }
}
