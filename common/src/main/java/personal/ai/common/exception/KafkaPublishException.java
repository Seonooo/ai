package personal.ai.common.exception;

/**
 * Kafka Publish Exception
 * Kafka 메시지 발행 실패 시 발생
 */
public class KafkaPublishException extends RuntimeException {

    public KafkaPublishException(String message) {
        super(message);
    }

    public KafkaPublishException(String message, Throwable cause) {
        super(message, cause);
    }

    public static KafkaPublishException publishFailed(String topic, Throwable cause) {
        return new KafkaPublishException(
                String.format("Failed to publish message to topic: %s", topic), cause);
    }
}
