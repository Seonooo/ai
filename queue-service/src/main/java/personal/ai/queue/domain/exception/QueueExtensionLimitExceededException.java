package personal.ai.queue.domain.exception;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

/**
 * 대기열 토큰 연장 한도를 초과했을 때 발생하는 예외
 */
public class QueueExtensionLimitExceededException extends BusinessException {

    public QueueExtensionLimitExceededException(String concertId, String userId) {
        super(ErrorCode.QUEUE_EXTENSION_LIMIT_EXCEEDED,
              "concertId: %s, userId: %s".formatted(concertId, userId));
    }
}
