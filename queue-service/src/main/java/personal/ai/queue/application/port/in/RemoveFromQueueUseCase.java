package personal.ai.queue.application.port.in;

/**
 * Remove From Queue Use Case
 * 결제 완료 시 대기열에서 유저 제거
 */
public interface RemoveFromQueueUseCase {

    /**
     * Active Queue에서 유저 제거
     * @param command 제거 명령
     */
    void removeFromQueue(RemoveFromQueueCommand command);

    /**
     * 대기열 제거 명령
     */
    record RemoveFromQueueCommand(
            String concertId,
            String userId
    ) {}
}
