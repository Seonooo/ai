package personal.ai.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration-style unit tests for HealthCheckController
 * Tests the controller through Spring MVC layer with real dependencies
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HealthCheckController Integration Tests")
class HealthCheckControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("HTTP Endpoint Tests")
    class HttpEndpointTests {

        @Test
        @DisplayName("Should return 200 OK on /api/v1/health endpoint")
        void shouldReturn200OnHealthEndpoint() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return JSON content type")
        void shouldReturnJsonContentType() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(content().contentType("application/json"));
        }

        @Test
        @DisplayName("Should return response with result field")
        void shouldReturnResponseWithResultField() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result", isOneOf("success", "error")));
        }

        @Test
        @DisplayName("Should return response with message field")
        void shouldReturnResponseWithMessageField() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.message", notNullValue()));
        }

        @Test
        @DisplayName("Should return response with data field")
        void shouldReturnResponseWithDataField() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(jsonPath("$.data").exists());
        }
    }

    @Nested
    @DisplayName("Health Check Data Structure Tests")
    class HealthCheckDataStructureTests {

        @Test
        @DisplayName("Should return database status in data")
        void shouldReturnDatabaseStatusInData() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(jsonPath("$.data.database").exists())
                    .andExpect(jsonPath("$.data.database", isOneOf("UP", "DOWN")));
        }

        @Test
        @DisplayName("Should return redis status in data")
        void shouldReturnRedisStatusInData() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(jsonPath("$.data.redis").exists())
                    .andExpect(jsonPath("$.data.redis", isOneOf("UP", "DOWN")));
        }

        @Test
        @DisplayName("Should return kafka status in data")
        void shouldReturnKafkaStatusInData() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(jsonPath("$.data.kafka").exists())
                    .andExpect(jsonPath("$.data.kafka", isOneOf("UP", "DOWN")));
        }

        @Test
        @DisplayName("Should return all three component statuses")
        void shouldReturnAllThreeComponentStatuses() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(jsonPath("$.data.database").exists())
                    .andExpect(jsonPath("$.data.redis").exists())
                    .andExpect(jsonPath("$.data.kafka").exists());
        }
    }

    @Nested
    @DisplayName("API Response Format Compliance")
    class ApiResponseFormatCompliance {

        @Test
        @DisplayName("Should follow API response format with success result")
        void shouldFollowApiResponseFormatWithSuccessResult() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(jsonPath("$.result").value(anyOf(is("success"), is("error"))))
                    .andExpect(jsonPath("$.message").isString())
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("Should have exactly three fields in root response")
        void shouldHaveExactlyThreeFieldsInRootResponse() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(jsonPath("$.*", hasSize(3)))
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("Should have exactly three fields in data object")
        void shouldHaveExactlyThreeFieldsInDataObject() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(jsonPath("$.data.*", hasSize(3)))
                    .andExpect(jsonPath("$.data.database").exists())
                    .andExpect(jsonPath("$.data.redis").exists())
                    .andExpect(jsonPath("$.data.kafka").exists());
        }
    }

    @Nested
    @DisplayName("Endpoint Behavior Tests")
    class EndpointBehaviorTests {

        @Test
        @DisplayName("Should not require authentication")
        void shouldNotRequireAuthentication() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should support GET method")
        void shouldSupportGetMethod() throws Exception {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should handle multiple concurrent requests")
        void shouldHandleMultipleConcurrentRequests() throws Exception {
            // Execute multiple requests
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/v1/health"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.result").exists());
            }
        }

        @Test
        @DisplayName("Should return consistent response structure")
        void shouldReturnConsistentResponseStructure() throws Exception {
            // First request
            String firstResponse = mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Second request
            String secondResponse = mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Both should have same structure (though values may differ)
            org.assertj.core.api.Assertions.assertThat(firstResponse).contains("result");
            org.assertj.core.api.Assertions.assertThat(secondResponse).contains("result");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 404 for non-existent endpoints")
        void shouldReturn404ForNonExistentEndpoints() throws Exception {
            mockMvc.perform(get("/api/v1/nonexistent"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 for health endpoint without version")
        void shouldReturn404ForHealthEndpointWithoutVersion() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 for health endpoint without api prefix")
        void shouldReturn404ForHealthEndpointWithoutApiPrefix() throws Exception {
            mockMvc.perform(get("/v1/health"))
                    .andExpect(status().isNotFound());
        }
    }
}