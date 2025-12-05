package personal.ai.queue.application.port.in;

import personal.ai.queue.domain.model.QueueToken;

/**
 * 대기열 상태 조회 UseCase (Input Port)
 */
public interface GetQueueStatusUseCase {

    /**
     * 대기열 상태 조회
     * @param query 상태 조회 쿼리
     * @return 토큰 정보
     */
    QueueToken getStatus(GetQueueStatusQuery query);

    /**
     * 상태 조회 쿼리
     */
    record GetQueueStatusQuery(
            String concertId,
            String userId
    ) {}
}
