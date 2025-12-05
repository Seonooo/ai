package personal.ai.queue.adapter.in.web.dto;

import personal.ai.queue.domain.model.QueueStatus;
import personal.ai.queue.domain.model.QueueToken;

import java.time.Instant;

/**
 * 대기열 토큰 응답
 */
public record QueueTokenResponse(
        String concertId,
        String userId,
        String token,
        QueueStatus status,
        Long position,
        Instant expiredAt,
        Integer extendCount,
        Long recommendedPollIntervalMs,  // 권장 폴링 간격 (밀리초)
        Long minPollIntervalMs            // 최소 폴링 간격 (밀리초, Rate Limit)
) {
    public static QueueTokenResponse from(QueueToken queueToken) {
        return from(queueToken, null, null);
    }

    public static QueueTokenResponse from(QueueToken queueToken,
                                          Long recommendedPollIntervalMs,
                                          Long minPollIntervalMs) {
        return new QueueTokenResponse(
                queueToken.concertId(),
                queueToken.userId(),
                queueToken.token(),
                queueToken.status(),
                queueToken.position(),
                queueToken.expiredAt(),
                queueToken.extendCount(),
                recommendedPollIntervalMs,
                minPollIntervalMs
        );
    }
}
