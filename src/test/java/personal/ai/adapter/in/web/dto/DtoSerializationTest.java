package personal.ai.adapter.in.web.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DTO JSON serialization/deserialization
 * Verifies DTOs can be properly serialized to/from JSON
 */
@DisplayName("DTO Serialization Tests")
class DtoSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("ApiResponse Serialization")
    class ApiResponseSerialization {

        @Test
        @DisplayName("Should serialize success response to JSON")
        void shouldSerializeSuccessResponseToJson() throws JsonProcessingException {
            // Given
            ApiResponse<String> response = ApiResponse.success("Operation successful", "data123");

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("\"result\":\"success\"");
            assertThat(json).contains("\"message\":\"Operation successful\"");
            assertThat(json).contains("\"data\":\"data123\"");
        }

        @Test
        @DisplayName("Should serialize error response to JSON")
        void shouldSerializeErrorResponseToJson() throws JsonProcessingException {
            // Given
            ApiResponse<String> response = ApiResponse.error("Operation failed", "error details");

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("\"result\":\"error\"");
            assertThat(json).contains("\"message\":\"Operation failed\"");
            assertThat(json).contains("\"data\":\"error details\"");
        }

        @Test
        @DisplayName("Should serialize response with null data")
        void shouldSerializeResponseWithNullData() throws JsonProcessingException {
            // Given
            ApiResponse<Void> response = ApiResponse.success("No content");

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("\"result\":\"success\"");
            assertThat(json).contains("\"data\":null");
        }

        @Test
        @DisplayName("Should deserialize JSON to ApiResponse")
        void shouldDeserializeJsonToApiResponse() throws JsonProcessingException {
            // Given
            String json = "{\"result\":\"success\",\"message\":\"Test\",\"data\":\"value\"}";

            // When
            @SuppressWarnings("unchecked")
            ApiResponse<String> response = objectMapper.readValue(json, ApiResponse.class);

            // Then
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Test");
            assertThat(response.data()).isEqualTo("value");
        }

        @Test
        @DisplayName("Should handle nested object serialization")
        void shouldHandleNestedObjectSerialization() throws JsonProcessingException {
            // Given
            HealthCheckResponse healthData = new HealthCheckResponse("UP", "DOWN", "UP");
            ApiResponse<HealthCheckResponse> response = ApiResponse.success("Health check", healthData);

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("\"result\":\"success\"");
            assertThat(json).contains("\"database\":\"UP\"");
            assertThat(json).contains("\"redis\":\"DOWN\"");
            assertThat(json).contains("\"kafka\":\"UP\"");
        }
    }

    @Nested
    @DisplayName("HealthCheckResponse Serialization")
    class HealthCheckResponseSerialization {

        @Test
        @DisplayName("Should serialize HealthCheckResponse to JSON")
        void shouldSerializeHealthCheckResponseToJson() throws JsonProcessingException {
            // Given
            HealthCheckResponse response = new HealthCheckResponse("UP", "UP", "UP");

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("\"database\":\"UP\"");
            assertThat(json).contains("\"redis\":\"UP\"");
            assertThat(json).contains("\"kafka\":\"UP\"");
        }

        @Test
        @DisplayName("Should serialize with DOWN statuses")
        void shouldSerializeWithDownStatuses() throws JsonProcessingException {
            // Given
            HealthCheckResponse response = new HealthCheckResponse("DOWN", "DOWN", "DOWN");

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("\"database\":\"DOWN\"");
            assertThat(json).contains("\"redis\":\"DOWN\"");
            assertThat(json).contains("\"kafka\":\"DOWN\"");
        }

        @Test
        @DisplayName("Should serialize with mixed statuses")
        void shouldSerializeWithMixedStatuses() throws JsonProcessingException {
            // Given
            HealthCheckResponse response = new HealthCheckResponse("UP", "DOWN", "UP");

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("\"database\":\"UP\"");
            assertThat(json).contains("\"redis\":\"DOWN\"");
            assertThat(json).contains("\"kafka\":\"UP\"");
        }

        @Test
        @DisplayName("Should deserialize JSON to HealthCheckResponse")
        void shouldDeserializeJsonToHealthCheckResponse() throws JsonProcessingException {
            // Given
            String json = "{\"database\":\"UP\",\"redis\":\"DOWN\",\"kafka\":\"UP\"}";

            // When
            HealthCheckResponse response = objectMapper.readValue(json, HealthCheckResponse.class);

            // Then
            assertThat(response.database()).isEqualTo("UP");
            assertThat(response.redis()).isEqualTo("DOWN");
            assertThat(response.kafka()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should handle null values in serialization")
        void shouldHandleNullValuesInSerialization() throws JsonProcessingException {
            // Given
            HealthCheckResponse response = new HealthCheckResponse(null, "UP", null);

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertThat(json).contains("\"database\":null");
            assertThat(json).contains("\"redis\":\"UP\"");
            assertThat(json).contains("\"kafka\":null");
        }

        @Test
        @DisplayName("Should preserve field order in JSON")
        void shouldPreserveFieldOrderInJson() throws JsonProcessingException {
            // Given
            HealthCheckResponse response = new HealthCheckResponse("UP", "DOWN", "UP");

            // When
            String json = objectMapper.writeValueAsString(response);
            int dbIndex = json.indexOf("database");
            int redisIndex = json.indexOf("redis");
            int kafkaIndex = json.indexOf("kafka");

            // Then - fields appear in declaration order
            assertThat(dbIndex).isLessThan(redisIndex);
            assertThat(redisIndex).isLessThan(kafkaIndex);
        }
    }

    @Nested
    @DisplayName("Complete API Response Serialization")
    class CompleteApiResponseSerialization {

        @Test
        @DisplayName("Should serialize complete health check API response")
        void shouldSerializeCompleteHealthCheckApiResponse() throws JsonProcessingException {
            // Given
            HealthCheckResponse healthData = new HealthCheckResponse("UP", "UP", "UP");
            ApiResponse<HealthCheckResponse> apiResponse = 
                    ApiResponse.success("Application is healthy", healthData);

            // When
            String json = objectMapper.writeValueAsString(apiResponse);

            // Then
            assertThat(json).isNotEmpty();
            assertThat(json).contains("\"result\":\"success\"");
            assertThat(json).contains("\"message\":\"Application is healthy\"");
            assertThat(json).contains("\"data\":");
            assertThat(json).contains("\"database\":\"UP\"");
            assertThat(json).contains("\"redis\":\"UP\"");
            assertThat(json).contains("\"kafka\":\"UP\"");
        }

        @Test
        @DisplayName("Should serialize error health check API response")
        void shouldSerializeErrorHealthCheckApiResponse() throws JsonProcessingException {
            // Given
            HealthCheckResponse healthData = new HealthCheckResponse("DOWN", "UP", "UP");
            ApiResponse<HealthCheckResponse> apiResponse = 
                    ApiResponse.error("Some components are unhealthy", healthData);

            // When
            String json = objectMapper.writeValueAsString(apiResponse);

            // Then
            assertThat(json).contains("\"result\":\"error\"");
            assertThat(json).contains("\"message\":\"Some components are unhealthy\"");
            assertThat(json).contains("\"database\":\"DOWN\"");
        }

        @Test
        @DisplayName("Should deserialize complete API response from JSON")
        void shouldDeserializeCompleteApiResponseFromJson() throws JsonProcessingException {
            // Given
            String json = "{" +
                    "\"result\":\"success\"," +
                    "\"message\":\"Application is healthy\"," +
                    "\"data\":{" +
                    "\"database\":\"UP\"," +
                    "\"redis\":\"UP\"," +
                    "\"kafka\":\"UP\"" +
                    "}" +
                    "}";

            // When
            @SuppressWarnings("unchecked")
            ApiResponse<HealthCheckResponse> response = objectMapper.readValue(json, 
                    objectMapper.getTypeFactory().constructParametricType(
                            ApiResponse.class, HealthCheckResponse.class));

            // Then
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Application is healthy");
        }
    }
}