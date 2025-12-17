package personal.ai.queue.adapter.in.web.dto;

import personal.ai.queue.domain.model.QueuePosition;

/**
 * 대기 순번 응답
 */
public record QueuePositionResponse(
        String concertId,
        String userId,
        Long position,
        Long totalWaiting,
        Integer estimatedWaitMinutes,
        String status,
        String token,
        boolean isNewEntry) {
    public static QueuePositionResponse from(QueuePosition queuePosition) {
        return new QueuePositionResponse(
                queuePosition.concertId(),
                queuePosition.userId(),
                queuePosition.position(),
                queuePosition.totalWaiting(),
                queuePosition.estimatedWaitMinutes(),
                queuePosition.status(),
                queuePosition.token(),
                queuePosition.isNewEntry());
    }
}
