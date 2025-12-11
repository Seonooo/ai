package personal.ai.common.exception;

/**
 * Outbox Event Exception
 * Outbox 이벤트 저장 실패 시 발생
 */
public class OutboxEventException extends RuntimeException {

    public OutboxEventException(String message) {
        super(message);
    }

    public OutboxEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public static OutboxEventException saveFailed(Long aggregateId, Throwable cause) {
        return new OutboxEventException(
                String.format("Failed to save outbox event for aggregate: %d", aggregateId), cause);
    }
}
