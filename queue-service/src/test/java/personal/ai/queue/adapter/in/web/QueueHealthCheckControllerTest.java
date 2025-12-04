package personal.ai.queue.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.springframework.data.redis.core.RedisCallback;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Queue Service Health Check Controller 단위 테스트
 * agent.md Testing Strategy - BDD Style (Given-When-Then)
 * @WebMvcTest를 사용하여 컨트롤러 계층만 테스트
 */
@WebMvcTest(QueueHealthCheckController.class)
@DisplayName("Queue Service Health Check API 단위 테스트")
class QueueHealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("헬스 체크 API는 정상 응답을 반환한다")
    void healthCheckReturnsSuccess() throws Exception {
        // Given: Redis가 정상 동작 중
        given(redisTemplate.execute(any(RedisCallback.class))).willReturn("PONG");

        // When: 헬스 체크 엔드포인트를 호출하면
        mockMvc.perform(get("/api/v1/health")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: 200 OK와 함께 정상 응답을 반환한다
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.result").value("success"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.redis").value("UP"));
    }

    @Test
    @DisplayName("헬스 체크 응답은 ApiResponse 포맷을 따른다")
    void healthCheckFollowsApiResponseFormat() throws Exception {
        // Given: agent.md API Design Guidelines
        given(redisTemplate.execute(any(RedisCallback.class))).willReturn("PONG");

        // When: 헬스 체크를 호출하면
        mockMvc.perform(get("/api/v1/health"))
                // Then: ApiResponse<T> 포맷을 따른다
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isString())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("Queue Service는 Redis 상태만 체크한다")
    void queueServiceOnlyChecksRedis() throws Exception {
        // Given: Queue Service는 경량 서비스
        given(redisTemplate.execute(any(RedisCallback.class))).willReturn("PONG");

        // When: 헬스 체크를 호출하면
        mockMvc.perform(get("/api/v1/health"))
                // Then: Redis 상태만 포함된다
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.redis").exists())
                .andExpect(jsonPath("$.data.database").doesNotExist());
    }
}
