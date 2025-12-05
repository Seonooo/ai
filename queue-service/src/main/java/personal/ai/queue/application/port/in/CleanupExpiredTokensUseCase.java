package personal.ai.queue.application.port.in;

/**
 * 만료된 토큰 정리 UseCase (Input Port)
 * 스케줄러가 주기적으로 호출
 */
public interface CleanupExpiredTokensUseCase {

    /**
     * 특정 콘서트의 만료된 토큰 정리
     * @param concertId 콘서트 ID
     * @return 정리된 토큰 수
     */
    long cleanupExpired(String concertId);

    /**
     * 모든 콘서트의 만료된 토큰 정리
     * @return 총 정리된 토큰 수
     */
    long cleanupAllConcerts();
}
