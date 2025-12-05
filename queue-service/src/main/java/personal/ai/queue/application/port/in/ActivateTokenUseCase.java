package personal.ai.queue.application.port.in;

import personal.ai.queue.domain.model.QueueToken;

/**
 * 토큰 활성화 UseCase (Input Port)
 * 유저가 예매 페이지에 최초 접속할 때 READY -> ACTIVE 전환
 */
public interface ActivateTokenUseCase {

    /**
     * 토큰을 활성 상태로 전환하고 TTL을 10분으로 연장
     * @param command 활성화 커맨드
     * @return 활성화된 토큰 정보
     */
    QueueToken activate(ActivateTokenCommand command);

    /**
     * 토큰 활성화 커맨드
     */
    record ActivateTokenCommand(
            String concertId,
            String userId
    ) {}
}
