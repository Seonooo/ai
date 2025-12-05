package personal.ai.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Queue Service Application
 * 대규모 트래픽을 처리하는 대기열 시스템
 * Redis ZSet + Hash 하이브리드 구조 사용
 */
@SpringBootApplication(scanBasePackages = {
        "personal.ai.queue",
        "personal.ai.common"  // common 모듈의 GlobalExceptionHandler 등을 스캔
})
public class QueueServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueueServiceApplication.class, args);
    }
}
