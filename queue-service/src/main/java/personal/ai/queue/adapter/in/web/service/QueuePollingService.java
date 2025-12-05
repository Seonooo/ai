package personal.ai.queue.adapter.in.web.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import personal.ai.queue.adapter.in.web.dto.QueueTokenResponse;
import personal.ai.queue.application.config.QueueConfigProperties;
import personal.ai.queue.application.port.in.GetQueueStatusUseCase;
import personal.ai.queue.domain.model.QueueStatus;
import personal.ai.queue.domain.model.QueueToken;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Queue Polling Service (SSE)
 * 클라이언트에게 대기열 상태를 실시간으로 전송
 * 동적 폴링 간격 지원 (순번 1~1000: 3초, 1001~: 10초)
 */
@Slf4j
@Service
public class QueuePollingService {

    // 상수
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30분
    private static final String EVENT_TYPE_STATUS_UPDATE = "status-update";
    private static final String EVENT_TYPE_READY = "ready";
    private static final String EVENT_TYPE_ERROR = "error";
    private final GetQueueStatusUseCase getQueueStatusUseCase;
    private final QueueConfigProperties configProperties;
    // SSE 연결 관리 (concertId:userId -> SseEmitter)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    // 폴링 스케줄 관리 (연결별로 스케줄을 취소할 수 있도록)
    private final Map<String, ScheduledFuture<?>> pollingSchedules = new ConcurrentHashMap<>();
    // Virtual Thread Executor for polling
    private final ScheduledExecutorService executor;
    /**
     * 생성자: 설정값으로부터 Executor 초기화
     */
    public QueuePollingService(GetQueueStatusUseCase getQueueStatusUseCase,
                               QueueConfigProperties configProperties) {
        this.getQueueStatusUseCase = getQueueStatusUseCase;
        this.configProperties = configProperties;

        int poolSize = configProperties.polling().executorPoolSize();
        this.executor = Executors.newScheduledThreadPool(poolSize, Thread.ofVirtual().factory());

        log.info("QueuePollingService initialized with executor pool size: {}", poolSize);
    }

    /**
     * 클라이언트 구독 시작
     */
    public SseEmitter subscribe(String concertId, String userId) {
        String key = generateKey(concertId, userId);

        // 기존 연결이 있다면 완전히 정리 (emitter + polling schedule)
        cleanupConnection(key);

        // 새 SSE Emitter 생성
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.put(key, emitter);

        log.info("SSE subscription started: concertId={}, userId={}", concertId, userId);

        // 연결 종료 시 정리
        emitter.onCompletion(() -> {
            cleanupConnection(key);
            log.info("SSE connection completed: concertId={}, userId={}", concertId, userId);
        });

        emitter.onTimeout(() -> {
            cleanupConnection(key);
            log.info("SSE connection timeout: concertId={}, userId={}", concertId, userId);
        });

        emitter.onError((e) -> {
            cleanupConnection(key);
            log.error("SSE connection error: concertId={}, userId={}", concertId, userId, e);
        });

        // 최초 상태 전송
        sendInitialStatus(concertId, userId, emitter);

        // 주기적 폴링 시작 (Virtual Thread)
        startPolling(concertId, userId, key);

        return emitter;
    }

    /**
     * 최초 상태 전송
     */
    private void sendInitialStatus(String concertId, String userId, SseEmitter emitter) {
        try {
            QueueToken token = getQueueStatusUseCase.getStatus(
                    new GetQueueStatusUseCase.GetQueueStatusQuery(concertId, userId)
            );

            QueueTokenResponse response = createResponseWithPollingInterval(token);
            emitter.send(SseEmitter.event()
                    .name(EVENT_TYPE_STATUS_UPDATE)
                    .data(response));

            log.debug("Initial status sent: concertId={}, userId={}, status={}, recommendedInterval={}ms",
                    concertId, userId, token.status(), response.recommendedPollIntervalMs());

        } catch (IOException e) {
            log.error("Failed to send initial status: concertId={}, userId={}",
                    concertId, userId, e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 동적 폴링 시작 (재귀적 스케줄링)
     * 각 폴링마다 상태에 따라 다음 폴링 간격을 동적으로 조정
     */
    private void startPolling(String concertId, String userId, String key) {
        // 초기 폴링 간격은 빠른 폴링으로 시작
        long initialPollingInterval = configProperties.polling().fastIntervalMs();
        scheduleNextPoll(concertId, userId, key, initialPollingInterval);
    }

    /**
     * 다음 폴링 스케줄링 (재귀적)
     */
    private void scheduleNextPoll(String concertId, String userId, String key, long delay) {
        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                SseEmitter emitter = emitters.get(key);
                if (emitter == null) {
                    // 연결이 끊어졌으면 폴링 중지
                    pollingSchedules.remove(key);
                    log.debug("Polling stopped: connection closed - concertId={}, userId={}",
                            concertId, userId);
                    return;
                }

                // 현재 상태 조회
                QueueToken token = getQueueStatusUseCase.getStatus(
                        new GetQueueStatusUseCase.GetQueueStatusQuery(concertId, userId)
                );

                // READY 상태가 되면 특별 이벤트 전송 후 종료
                if (token.status() == QueueStatus.READY || token.status() == QueueStatus.ACTIVE) {
                    sendReadyEvent(emitter, token);
                    emitter.complete();
                    cleanupConnection(key);
                    log.info("Polling completed: user became READY - concertId={}, userId={}",
                            concertId, userId);
                    return;
                }

                // 일반 상태 업데이트
                sendStatusUpdate(emitter, token);

                // 다음 폴링 간격 계산 (현재 상태 기반)
                long nextInterval = calculateRecommendedPollInterval(token);

                log.debug("Next poll scheduled: concertId={}, userId={}, position={}, nextInterval={}ms",
                        concertId, userId, token.position(), nextInterval);

                // 다음 폴링 재귀적으로 스케줄링
                scheduleNextPoll(concertId, userId, key, nextInterval);

            } catch (Exception e) {
                log.error("Polling error: concertId={}, userId={}", concertId, userId, e);
                SseEmitter emitter = emitters.get(key);
                if (emitter != null) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(EVENT_TYPE_ERROR)
                                .data("Polling error occurred"));
                        emitter.completeWithError(e);
                    } catch (IOException ex) {
                        log.error("Failed to send error event", ex);
                    }
                }
                cleanupConnection(key);
            }
        }, delay, TimeUnit.MILLISECONDS);

        // 스케줄 저장 (이전 스케줄이 있다면 취소)
        ScheduledFuture<?> oldFuture = pollingSchedules.put(key, future);
        if (oldFuture != null && !oldFuture.isDone()) {
            oldFuture.cancel(false);
            log.debug("Previous polling schedule cancelled: key={}", key);
        }
    }

    /**
     * 상태 업데이트 전송
     */
    private void sendStatusUpdate(SseEmitter emitter, QueueToken token) throws IOException {
        QueueTokenResponse response = createResponseWithPollingInterval(token);
        emitter.send(SseEmitter.event()
                .name(EVENT_TYPE_STATUS_UPDATE)
                .data(response));

        log.debug("Status update sent: status={}, position={}, recommendedInterval={}ms",
                token.status(), token.position(), response.recommendedPollIntervalMs());
    }

    /**
     * READY 이벤트 전송 (예매 페이지 진입 가능)
     */
    private void sendReadyEvent(SseEmitter emitter, QueueToken token) throws IOException {
        QueueTokenResponse response = createResponseWithPollingInterval(token);
        emitter.send(SseEmitter.event()
                .name(EVENT_TYPE_READY)
                .data(response));

        log.info("Ready event sent: concertId={}, userId={}, token={}",
                token.concertId(), token.userId(), token.token());
    }

    /**
     * 키 생성
     */
    private String generateKey(String concertId, String userId) {
        return concertId + ":" + userId;
    }

    /**
     * 연결 정리 (emitter + polling schedule)
     */
    private void cleanupConnection(String key) {
        // Emitter 제거
        emitters.remove(key);

        // 스케줄된 폴링 취소
        ScheduledFuture<?> future = pollingSchedules.remove(key);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.debug("Polling schedule cancelled: key={}", key);
        }
    }

    /**
     * 동적 폴링 간격 계산
     * - WAITING 상태: 순번에 따라 3초 또는 10초
     * - READY/ACTIVE 상태: 빠른 폴링 (3초)
     * - 기타 상태: 느린 폴링 (10초)
     */
    private long calculateRecommendedPollInterval(QueueToken token) {
        QueueConfigProperties.Polling pollingConfig = configProperties.polling();

        // READY 또는 ACTIVE 상태는 빠른 폴링
        if (token.status() == QueueStatus.READY || token.status() == QueueStatus.ACTIVE) {
            return pollingConfig.fastIntervalMs();
        }

        // WAITING 상태는 순번에 따라 차등
        if (token.status() == QueueStatus.WAITING && token.position() != null) {
            if (token.position() <= pollingConfig.fastThreshold()) {
                return pollingConfig.fastIntervalMs();  // 1~1000번: 3초
            } else {
                return pollingConfig.slowIntervalMs();  // 1001번 이상: 10초
            }
        }

        // 기타 상태 (EXPIRED, NOT_FOUND 등)는 느린 폴링
        return pollingConfig.slowIntervalMs();
    }

    /**
     * 폴링 간격 정보를 포함한 응답 생성
     */
    private QueueTokenResponse createResponseWithPollingInterval(QueueToken token) {
        long recommendedInterval = calculateRecommendedPollInterval(token);
        long minInterval = configProperties.polling().minIntervalMs();

        return QueueTokenResponse.from(token, recommendedInterval, minInterval);
    }

    /**
     * 애플리케이션 종료 시 리소스 정리
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down QueuePollingService...");

        // 모든 활성 연결 정리
        emitters.keySet().forEach(this::cleanupConnection);

        // Executor 종료
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time, forcing shutdown...");
                executor.shutdownNow();
            }
            log.info("QueuePollingService shutdown completed");
        } catch (InterruptedException e) {
            log.error("Executor shutdown interrupted", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
