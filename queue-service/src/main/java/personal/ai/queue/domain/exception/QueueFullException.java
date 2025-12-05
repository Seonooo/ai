package personal.ai.queue.domain.exception;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

/**
 * 대기열이 가득 찼을 때 발생하는 예외
 */
public class QueueFullException extends BusinessException {

    public QueueFullException(String concertId) {
        super(ErrorCode.QUEUE_FULL, "concertId: " + concertId);
    }
}
