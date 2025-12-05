package personal.ai.queue.domain.exception;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

/**
 * 대기열 토큰이 만료되었을 때 발생하는 예외
 */
public class QueueTokenExpiredException extends BusinessException {

    public QueueTokenExpiredException(String concertId, String userId) {
        super(ErrorCode.QUEUE_TOKEN_EXPIRED,
              "concertId: %s, userId: %s".formatted(concertId, userId));
    }
}
