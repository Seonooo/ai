package personal.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic Spring Boot application context test
 * Note: More comprehensive tests are in AiApplicationTest.java
 * This file serves as a smoke test to ensure the application starts correctly
 */
@SpringBootTest
@ActiveProfiles("test")
class AiApplicationTests {

    @Test
    void contextLoads() {
        // This test passes if the application context loads successfully
        // It's a basic smoke test to catch major configuration issues
    }

}
