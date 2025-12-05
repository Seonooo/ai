package personal.ai.queue.application.port.in;

/**
 * Wait -> Active 전환 UseCase (Input Port)
 * 스케줄러가 주기적으로 호출
 */
public interface MoveToActiveQueueUseCase {

    /**
     * 대기열에서 활성 큐로 유저 이동
     * @param concertId 콘서트 ID
     * @return 이동된 유저 수
     */
    int moveWaitingToActive(String concertId);

    /**
     * 모든 콘서트의 대기열 처리
     * @return 총 이동된 유저 수
     */
    int moveAllConcerts();
}
