package personal.ai.queue.application.port.in;

import personal.ai.queue.domain.model.QueueToken;

/**
 * 토큰 연장 UseCase (Input Port)
 */
public interface ExtendTokenUseCase {

    /**
     * 토큰 유효 시간 연장 (최대 2회)
     * @param command 연장 커맨드
     * @return 연장된 토큰 정보
     */
    QueueToken extend(ExtendTokenCommand command);

    /**
     * 토큰 연장 커맨드
     */
    record ExtendTokenCommand(
            String concertId,
            String userId
    ) {}
}
