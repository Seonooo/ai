package personal.ai.queue.domain.exception;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

/**
 * 대기열 데이터 무결성 오류 발생 시 예외
 * Redis 데이터 포맷이 손상되었거나 예상과 다른 경우 발생
 */
public class QueueDataCorruptionException extends BusinessException {

    public QueueDataCorruptionException() {
        super(ErrorCode.QUEUE_DATA_CORRUPTION);
    }

    public QueueDataCorruptionException(Throwable cause) {
        super(ErrorCode.QUEUE_DATA_CORRUPTION, cause);
    }
}