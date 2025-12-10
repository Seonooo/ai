package personal.ai.core.booking.adapter.out.external;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QueueServiceRestClientAdapter Fallback 메서드 단위 테스트
 *
 * Circuit Breaker가 OPEN 상태일 때 Fallback이 올바르게 동작하는지 검증
 */
@DisplayName("QueueService Fallback 메서드 단위 테스트")
class QueueServiceFallbackTest {

    private QueueServiceRestClientAdapter adapter;
    private Method fallbackMethod;

    @BeforeEach
    void setUp() throws Exception {
        // Circuit Breaker Registry 생성
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config =
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .failureRateThreshold(50f)
                        .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Adapter 생성 (RestClient는 null이어도 fallback 테스트에는 무관)
        adapter = new QueueServiceRestClientAdapter(null);

        // Reflection으로 private fallback 메서드 접근
        fallbackMethod = QueueServiceRestClientAdapter.class.getDeclaredMethod(
                "validateTokenFallback", Long.class, String.class, Exception.class);
        fallbackMethod.setAccessible(true);
    }

    @Test
    @DisplayName("Fallback 메서드는 QUEUE_SERVICE_UNAVAILABLE 예외를 반환한다")
    void fallback_shouldThrowQueueServiceUnavailable() {
        // Given: Circuit Breaker가 OPEN 상태 (CallNotPermittedException 발생)
        Exception circuitOpenException = CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.of("test", io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.ofDefaults()));

        // When & Then: Fallback 메서드 호출 시 QUEUE_SERVICE_UNAVAILABLE 예외 발생
        assertThatThrownBy(() -> fallbackMethod.invoke(adapter, 1L, "test-token", circuitOpenException))
                .hasCauseInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException cause = (BusinessException) exception.getCause();
                    assertThat(cause.getErrorCode()).isEqualTo(ErrorCode.QUEUE_SERVICE_UNAVAILABLE);
                });
    }

    @Test
    @DisplayName("Fallback 에러 메시지는 SLO Decision을 반영한다 (Fairness > Availability)")
    void fallback_errorMessage_shouldReflectSLODecision() throws Exception {
        // Given: Circuit Breaker가 OPEN 상태
        Exception circuitOpenException = CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.of("test", io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.ofDefaults()));

        // When & Then: Fallback 에러 메시지 검증
        try {
            fallbackMethod.invoke(adapter, 1L, "test-token", circuitOpenException);
        } catch (Exception e) {
            BusinessException cause = (BusinessException) e.getCause();

            // Then: 503 Service Unavailable
            assertThat(cause.getErrorCode().getHttpStatus())
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

            // Then: 에러 코드 E003
            assertThat(cause.getErrorCode().getCode())
                    .isEqualTo("E003");

            // Then: 사용자 친화적 메시지 (대기열 언급, 재시도 안내)
            assertThat(cause.getMessage())
                    .contains("대기열")
                    .contains("5초 후 다시 시도해주세요");
        }
    }

    @Test
    @DisplayName("Fallback은 Fairness(공정성)를 우선시한다 - Fail-Fast 정책")
    void fallback_shouldPrioritizeFairness_overAvailability() throws Exception {
        // Given: Circuit Breaker OPEN (Queue Service 장애)
        Exception circuitOpenException = CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.of("test", io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.ofDefaults()));

        // When & Then: 503을 반환하여 대기열 없이 통과시키지 않음 (Fairness 100% 유지)
        try {
            fallbackMethod.invoke(adapter, 1L, "test-token", circuitOpenException);
        } catch (Exception e) {
            BusinessException cause = (BusinessException) e.getCause();

            // Then: 503 반환 (대기열 우회 차단 = Fairness 유지)
            assertThat(cause.getErrorCode().getHttpStatus())
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

            // Then: 가용성을 희생하더라도 공정성을 지킴
            assertThat(cause.getErrorCode())
                    .isEqualTo(ErrorCode.QUEUE_SERVICE_UNAVAILABLE);
        }
    }
}
