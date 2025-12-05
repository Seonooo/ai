package personal.ai.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Core Service Application
 * User, Booking, Payment 도메인을 포함하는 핵심 비즈니스 서비스
 */
@SpringBootApplication(scanBasePackages = {
        "personal.ai.core",
        "personal.ai.common"  // common 모듈의 GlobalExceptionHandler 등을 스캔
})
public class CoreServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
    }
}
