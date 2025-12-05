package personal.ai.queue.application.port.in;

import personal.ai.queue.domain.model.QueuePosition;

/**
 * 대기열 진입 UseCase (Input Port)
 */
public interface EnterQueueUseCase {

    /**
     * 대기열에 진입
     * @param command 대기열 진입 커맨드
     * @return 대기 순번 정보
     */
    QueuePosition enter(EnterQueueCommand command);

    /**
     * 대기열 진입 커맨드
     */
    record EnterQueueCommand(
            String concertId,
            String userId
    ) {}
}
