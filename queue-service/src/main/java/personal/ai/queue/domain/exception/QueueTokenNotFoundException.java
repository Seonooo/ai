package personal.ai.queue.domain.exception;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

/**
 * 대기열 토큰을 찾을 수 없을 때 발생하는 예외
 */
public class QueueTokenNotFoundException extends BusinessException {

    public QueueTokenNotFoundException(String concertId, String userId) {
        super(ErrorCode.QUEUE_TOKEN_NOT_FOUND,
              "concertId: %s, userId: %s".formatted(concertId, userId));
    }
}
