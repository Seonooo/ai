package personal.ai.adapter.in.web.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApiResponse record
 * Tests the immutable response wrapper and factory methods
 */
@DisplayName("ApiResponse Unit Tests")
class ApiResponseTest {

    @Nested
    @DisplayName("Record Construction")
    class RecordConstruction {

        @Test
        @DisplayName("Should create ApiResponse with all fields")
        void shouldCreateWithAllFields() {
            // Given
            String result = "success";
            String message = "Operation completed";
            String data = "test data";

            // When
            ApiResponse<String> response = new ApiResponse<>(result, message, data);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.result()).isEqualTo(result);
            assertThat(response.message()).isEqualTo(message);
            assertThat(response.data()).isEqualTo(data);
        }

        @Test
        @DisplayName("Should allow null data")
        void shouldAllowNullData() {
            // When
            ApiResponse<String> response = new ApiResponse<>("success", "No data", null);

            // Then
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("No data");
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("Should be immutable record")
        void shouldBeImmutableRecord() {
            // Given
            ApiResponse<String> response = new ApiResponse<>("success", "Test", "data");

            // Then - verify getters exist (record property)
            assertThat(response.result()).isNotNull();
            assertThat(response.message()).isNotNull();
            assertThat(response.data()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Success Factory Methods")
    class SuccessFactoryMethods {

        @Test
        @DisplayName("Should create success response with data")
        void shouldCreateSuccessResponseWithData() {
            // Given
            String message = "User created successfully";
            String data = "user123";

            // When
            ApiResponse<String> response = ApiResponse.success(message, data);

            // Then
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.message()).isEqualTo(message);
            assertThat(response.data()).isEqualTo(data);
        }

        @Test
        @DisplayName("Should create success response with complex data type")
        void shouldCreateSuccessResponseWithComplexData() {
            // Given
            HealthCheckResponse healthData = new HealthCheckResponse("UP", "UP", "UP");

            // When
            ApiResponse<HealthCheckResponse> response = 
                    ApiResponse.success("Health check passed", healthData);

            // Then
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.data()).isEqualTo(healthData);
            assertThat(response.data().database()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should create success response without data")
        void shouldCreateSuccessResponseWithoutData() {
            // Given
            String message = "Operation completed";

            // When
            ApiResponse<Void> response = ApiResponse.success(message);

            // Then
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.message()).isEqualTo(message);
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("Should handle empty message in success")
        void shouldHandleEmptyMessageInSuccess() {
            // When
            ApiResponse<String> response = ApiResponse.success("", "data");

            // Then
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.message()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null data explicitly in success")
        void shouldHandleNullDataExplicitlyInSuccess() {
            // When
            ApiResponse<String> response = ApiResponse.success("Done", null);

            // Then
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.data()).isNull();
        }
    }

    @Nested
    @DisplayName("Error Factory Methods")
    class ErrorFactoryMethods {

        @Test
        @DisplayName("Should create error response with data")
        void shouldCreateErrorResponseWithData() {
            // Given
            String message = "Validation failed";
            String errorData = "Invalid email format";

            // When
            ApiResponse<String> response = ApiResponse.error(message, errorData);

            // Then
            assertThat(response.result()).isEqualTo("error");
            assertThat(response.message()).isEqualTo(message);
            assertThat(response.data()).isEqualTo(errorData);
        }

        @Test
        @DisplayName("Should create error response with complex error data")
        void shouldCreateErrorResponseWithComplexErrorData() {
            // Given
            HealthCheckResponse healthData = new HealthCheckResponse("DOWN", "UP", "UP");

            // When
            ApiResponse<HealthCheckResponse> response = 
                    ApiResponse.error("Database is down", healthData);

            // Then
            assertThat(response.result()).isEqualTo("error");
            assertThat(response.data()).isEqualTo(healthData);
            assertThat(response.data().database()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should create error response without data")
        void shouldCreateErrorResponseWithoutData() {
            // Given
            String message = "Internal server error";

            // When
            ApiResponse<Void> response = ApiResponse.error(message);

            // Then
            assertThat(response.result()).isEqualTo("error");
            assertThat(response.message()).isEqualTo(message);
            assertThat(response.data()).isNull();
        }

        @Test
        @DisplayName("Should handle empty error message")
        void shouldHandleEmptyErrorMessage() {
            // When
            ApiResponse<Void> response = ApiResponse.error("");

            // Then
            assertThat(response.result()).isEqualTo("error");
            assertThat(response.message()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null data in error")
        void shouldHandleNullDataInError() {
            // When
            ApiResponse<String> response = ApiResponse.error("Error occurred", null);

            // Then
            assertThat(response.result()).isEqualTo("error");
            assertThat(response.data()).isNull();
        }
    }

    @Nested
    @DisplayName("Type Safety and Generics")
    class TypeSafetyAndGenerics {

        @Test
        @DisplayName("Should support String type parameter")
        void shouldSupportStringTypeParameter() {
            // When
            ApiResponse<String> response = ApiResponse.success("Done", "string data");

            // Then
            assertThat(response.data()).isInstanceOf(String.class);
        }

        @Test
        @DisplayName("Should support Integer type parameter")
        void shouldSupportIntegerTypeParameter() {
            // When
            ApiResponse<Integer> response = ApiResponse.success("Count", 42);

            // Then
            assertThat(response.data()).isInstanceOf(Integer.class);
            assertThat(response.data()).isEqualTo(42);
        }

        @Test
        @DisplayName("Should support custom object type parameter")
        void shouldSupportCustomObjectTypeParameter() {
            // Given
            HealthCheckResponse healthCheck = new HealthCheckResponse("UP", "UP", "UP");

            // When
            ApiResponse<HealthCheckResponse> response = 
                    ApiResponse.success("Health OK", healthCheck);

            // Then
            assertThat(response.data()).isInstanceOf(HealthCheckResponse.class);
        }

        @Test
        @DisplayName("Should support Void type parameter")
        void shouldSupportVoidTypeParameter() {
            // When
            ApiResponse<Void> response = ApiResponse.success("No content");

            // Then
            assertThat(response.data()).isNull();
        }
    }

    @Nested
    @DisplayName("Record Equality and HashCode")
    class RecordEqualityAndHashCode {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            ApiResponse<String> response1 = new ApiResponse<>("success", "Test", "data");
            ApiResponse<String> response2 = new ApiResponse<>("success", "Test", "data");

            // Then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when result differs")
        void shouldNotBeEqualWhenResultDiffers() {
            // Given
            ApiResponse<String> response1 = new ApiResponse<>("success", "Test", "data");
            ApiResponse<String> response2 = new ApiResponse<>("error", "Test", "data");

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when message differs")
        void shouldNotBeEqualWhenMessageDiffers() {
            // Given
            ApiResponse<String> response1 = new ApiResponse<>("success", "Message1", "data");
            ApiResponse<String> response2 = new ApiResponse<>("success", "Message2", "data");

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when data differs")
        void shouldNotBeEqualWhenDataDiffers() {
            // Given
            ApiResponse<String> response1 = new ApiResponse<>("success", "Test", "data1");
            ApiResponse<String> response2 = new ApiResponse<>("success", "Test", "data2");

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should handle null in equality")
        void shouldHandleNullInEquality() {
            // Given
            ApiResponse<String> response1 = new ApiResponse<>("success", "Test", null);
            ApiResponse<String> response2 = new ApiResponse<>("success", "Test", null);

            // Then
            assertThat(response1).isEqualTo(response2);
        }
    }

    @Nested
    @DisplayName("Record toString")
    class RecordToString {

        @Test
        @DisplayName("Should generate meaningful toString")
        void shouldGenerateMeaningfulToString() {
            // Given
            ApiResponse<String> response = new ApiResponse<>("success", "Test", "data");

            // When
            String toString = response.toString();

            // Then
            assertThat(toString).contains("success");
            assertThat(toString).contains("Test");
            assertThat(toString).contains("data");
        }

        @Test
        @DisplayName("Should handle null data in toString")
        void shouldHandleNullDataInToString() {
            // Given
            ApiResponse<String> response = new ApiResponse<>("success", "Test", null);

            // When
            String toString = response.toString();

            // Then
            assertThat(toString).contains("success");
            assertThat(toString).contains("null");
        }
    }

    @Nested
    @DisplayName("Real-world Usage Scenarios")
    class RealWorldUsageScenarios {

        @Test
        @DisplayName("Should work for health check success scenario")
        void shouldWorkForHealthCheckSuccessScenario() {
            // Given
            HealthCheckResponse healthData = new HealthCheckResponse("UP", "UP", "UP");

            // When
            ApiResponse<HealthCheckResponse> response = 
                    ApiResponse.success("Application is healthy", healthData);

            // Then
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Application is healthy");
            assertThat(response.data().database()).isEqualTo("UP");
            assertThat(response.data().redis()).isEqualTo("UP");
            assertThat(response.data().kafka()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should work for health check failure scenario")
        void shouldWorkForHealthCheckFailureScenario() {
            // Given
            HealthCheckResponse healthData = new HealthCheckResponse("DOWN", "UP", "UP");

            // When
            ApiResponse<HealthCheckResponse> response = 
                    ApiResponse.error("Some components are unhealthy", healthData);

            // Then
            assertThat(response.result()).isEqualTo("error");
            assertThat(response.message()).isEqualTo("Some components are unhealthy");
            assertThat(response.data().database()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should work for operation with no return data")
        void shouldWorkForOperationWithNoReturnData() {
            // When
            ApiResponse<Void> response = ApiResponse.success("Resource deleted");

            // Then
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.message()).isEqualTo("Resource deleted");
            assertThat(response.data()).isNull();
        }
    }
}