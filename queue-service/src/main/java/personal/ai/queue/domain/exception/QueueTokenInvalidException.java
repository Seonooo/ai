package personal.ai.queue.domain.exception;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

/**
 * 유효하지 않은 대기열 토큰일 때 발생하는 예외
 */
public class QueueTokenInvalidException extends BusinessException {

    public QueueTokenInvalidException(String concertId, String userId) {
        super(ErrorCode.QUEUE_TOKEN_INVALID,
              "concertId: %s, userId: %s".formatted(concertId, userId));
    }
}
