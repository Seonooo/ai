package personal.ai.queue.application.port.in;

/**
 * 토큰 검증 UseCase (Input Port)
 * 예매/결제 API 호출 시 토큰 유효성 검증
 */
public interface ValidateTokenUseCase {

    /**
     * 토큰 유효성 검증
     * @param query 검증 쿼리
     * @throws personal.ai.queue.domain.exception.QueueTokenNotFoundException 토큰 없음
     * @throws personal.ai.queue.domain.exception.QueueTokenExpiredException 토큰 만료
     * @throws personal.ai.queue.domain.exception.QueueTokenInvalidException 토큰 무효
     */
    void validate(ValidateTokenQuery query);

    /**
     * 토큰 검증 쿼리
     */
    record ValidateTokenQuery(
            String concertId,
            String userId,
            String token
    ) {}
}
