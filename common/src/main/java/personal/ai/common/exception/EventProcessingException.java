package personal.ai.common.exception;

/**
 * Event Processing Exception
 * 이벤트 처리 실패 시 발생
 */
public class EventProcessingException extends RuntimeException {

    public EventProcessingException(String message) {
        super(message);
    }

    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public static EventProcessingException processingFailed(String eventType, Throwable cause) {
        return new EventProcessingException(
                String.format("Failed to process event: %s", eventType), cause);
    }
}
