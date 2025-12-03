package personal.ai.adapter.in.web.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HealthCheckResponse record
 * Tests the immutable health status data structure
 */
@DisplayName("HealthCheckResponse Unit Tests")
class HealthCheckResponseTest {

    @Nested
    @DisplayName("Record Construction")
    class RecordConstruction {

        @Test
        @DisplayName("Should create HealthCheckResponse with all UP status")
        void shouldCreateWithAllUpStatus() {
            // When
            HealthCheckResponse response = new HealthCheckResponse("UP", "UP", "UP");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.database()).isEqualTo("UP");
            assertThat(response.redis()).isEqualTo("UP");
            assertThat(response.kafka()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should create HealthCheckResponse with all DOWN status")
        void shouldCreateWithAllDownStatus() {
            // When
            HealthCheckResponse response = new HealthCheckResponse("DOWN", "DOWN", "DOWN");

            // Then
            assertThat(response.database()).isEqualTo("DOWN");
            assertThat(response.redis()).isEqualTo("DOWN");
            assertThat(response.kafka()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should create HealthCheckResponse with mixed status")
        void shouldCreateWithMixedStatus() {
            // When
            HealthCheckResponse response = new HealthCheckResponse("UP", "DOWN", "UP");

            // Then
            assertThat(response.database()).isEqualTo("UP");
            assertThat(response.redis()).isEqualTo("DOWN");
            assertThat(response.kafka()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should allow null values")
        void shouldAllowNullValues() {
            // When
            HealthCheckResponse response = new HealthCheckResponse(null, null, null);

            // Then
            assertThat(response.database()).isNull();
            assertThat(response.redis()).isNull();
            assertThat(response.kafka()).isNull();
        }

        @Test
        @DisplayName("Should allow mixed null and non-null values")
        void shouldAllowMixedNullAndNonNullValues() {
            // When
            HealthCheckResponse response = new HealthCheckResponse("UP", null, "DOWN");

            // Then
            assertThat(response.database()).isEqualTo("UP");
            assertThat(response.redis()).isNull();
            assertThat(response.kafka()).isEqualTo("DOWN");
        }
    }

    @Nested
    @DisplayName("Status Values")
    class StatusValues {

        @ParameterizedTest
        @ValueSource(strings = {"UP", "DOWN", "UNKNOWN", "DEGRADED"})
        @DisplayName("Should accept various status strings for database")
        void shouldAcceptVariousStatusStringsForDatabase(String status) {
            // When
            HealthCheckResponse response = new HealthCheckResponse(status, "UP", "UP");

            // Then
            assertThat(response.database()).isEqualTo(status);
        }

        @ParameterizedTest
        @ValueSource(strings = {"UP", "DOWN", "UNKNOWN", "DEGRADED"})
        @DisplayName("Should accept various status strings for redis")
        void shouldAcceptVariousStatusStringsForRedis(String status) {
            // When
            HealthCheckResponse response = new HealthCheckResponse("UP", status, "UP");

            // Then
            assertThat(response.redis()).isEqualTo(status);
        }

        @ParameterizedTest
        @ValueSource(strings = {"UP", "DOWN", "UNKNOWN", "DEGRADED"})
        @DisplayName("Should accept various status strings for kafka")
        void shouldAcceptVariousStatusStringsForKafka(String status) {
            // When
            HealthCheckResponse response = new HealthCheckResponse("UP", "UP", status);

            // Then
            assertThat(response.kafka()).isEqualTo(status);
        }

        @Test
        @DisplayName("Should handle empty strings")
        void shouldHandleEmptyStrings() {
            // When
            HealthCheckResponse response = new HealthCheckResponse("", "", "");

            // Then
            assertThat(response.database()).isEmpty();
            assertThat(response.redis()).isEmpty();
            assertThat(response.kafka()).isEmpty();
        }

        @Test
        @DisplayName("Should preserve case sensitivity")
        void shouldPreserveCaseSensitivity() {
            // When
            HealthCheckResponse response = new HealthCheckResponse("Up", "down", "UP");

            // Then
            assertThat(response.database()).isEqualTo("Up");
            assertThat(response.redis()).isEqualTo("down");
            assertThat(response.kafka()).isEqualTo("UP");
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            HealthCheckResponse response1 = new HealthCheckResponse("UP", "UP", "UP");
            HealthCheckResponse response2 = new HealthCheckResponse("UP", "UP", "UP");

            // Then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when database status differs")
        void shouldNotBeEqualWhenDatabaseStatusDiffers() {
            // Given
            HealthCheckResponse response1 = new HealthCheckResponse("UP", "UP", "UP");
            HealthCheckResponse response2 = new HealthCheckResponse("DOWN", "UP", "UP");

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when redis status differs")
        void shouldNotBeEqualWhenRedisStatusDiffers() {
            // Given
            HealthCheckResponse response1 = new HealthCheckResponse("UP", "UP", "UP");
            HealthCheckResponse response2 = new HealthCheckResponse("UP", "DOWN", "UP");

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when kafka status differs")
        void shouldNotBeEqualWhenKafkaStatusDiffers() {
            // Given
            HealthCheckResponse response1 = new HealthCheckResponse("UP", "UP", "UP");
            HealthCheckResponse response2 = new HealthCheckResponse("UP", "UP", "DOWN");

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should handle null in equality comparisons")
        void shouldHandleNullInEqualityComparisons() {
            // Given
            HealthCheckResponse response1 = new HealthCheckResponse(null, "UP", "UP");
            HealthCheckResponse response2 = new HealthCheckResponse(null, "UP", "UP");

            // Then
            assertThat(response1).isEqualTo(response2);
        }

        @Test
        @DisplayName("Should not be equal when one has null and other has value")
        void shouldNotBeEqualWhenOneHasNullAndOtherHasValue() {
            // Given
            HealthCheckResponse response1 = new HealthCheckResponse(null, "UP", "UP");
            HealthCheckResponse response2 = new HealthCheckResponse("UP", "UP", "UP");

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }
    }

    @Nested
    @DisplayName("Record toString")
    class RecordToString {

        @Test
        @DisplayName("Should generate meaningful toString")
        void shouldGenerateMeaningfulToString() {
            // Given
            HealthCheckResponse response = new HealthCheckResponse("UP", "DOWN", "UP");

            // When
            String toString = response.toString();

            // Then
            assertThat(toString).contains("UP");
            assertThat(toString).contains("DOWN");
            assertThat(toString).contains("database");
            assertThat(toString).contains("redis");
            assertThat(toString).contains("kafka");
        }

        @Test
        @DisplayName("Should handle null values in toString")
        void shouldHandleNullValuesInToString() {
            // Given
            HealthCheckResponse response = new HealthCheckResponse(null, "UP", null);

            // When
            String toString = response.toString();

            // Then
            assertThat(toString).contains("null");
            assertThat(toString).contains("UP");
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("Should be immutable record")
        void shouldBeImmutableRecord() {
            // Given
            HealthCheckResponse response = new HealthCheckResponse("UP", "UP", "UP");

            // Then - verify only getters exist (records are immutable by design)
            assertThat(response.database()).isEqualTo("UP");
            assertThat(response.redis()).isEqualTo("UP");
            assertThat(response.kafka()).isEqualTo("UP");

            // Creating new instance with different values
            HealthCheckResponse newResponse = new HealthCheckResponse("DOWN", "DOWN", "DOWN");
            
            // Original remains unchanged
            assertThat(response.database()).isEqualTo("UP");
            assertThat(newResponse.database()).isEqualTo("DOWN");
        }
    }

    @Nested
    @DisplayName("Real-world Scenarios")
    class RealWorldScenarios {

        @ParameterizedTest
        @CsvSource({
            "UP,UP,UP,true",
            "UP,DOWN,UP,false",
            "DOWN,UP,UP,false",
            "UP,UP,DOWN,false",
            "DOWN,DOWN,DOWN,false"
        })
        @DisplayName("Should correctly represent health status combinations")
        void shouldCorrectlyRepresentHealthStatusCombinations(
                String database, String redis, String kafka, boolean allHealthy) {
            // When
            HealthCheckResponse response = new HealthCheckResponse(database, redis, kafka);

            // Then
            boolean actuallyAllHealthy = "UP".equals(response.database()) &&
                                        "UP".equals(response.redis()) &&
                                        "UP".equals(response.kafka());
            assertThat(actuallyAllHealthy).isEqualTo(allHealthy);
        }

        @Test
        @DisplayName("Should represent complete system failure")
        void shouldRepresentCompleteSystemFailure() {
            // When
            HealthCheckResponse response = new HealthCheckResponse("DOWN", "DOWN", "DOWN");

            // Then
            assertThat(response.database()).isEqualTo("DOWN");
            assertThat(response.redis()).isEqualTo("DOWN");
            assertThat(response.kafka()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should represent partial system degradation")
        void shouldRepresentPartialSystemDegradation() {
            // When
            HealthCheckResponse response = new HealthCheckResponse("UP", "DOWN", "UP");

            // Then
            assertThat(response.database()).isEqualTo("UP");
            assertThat(response.redis()).isEqualTo("DOWN");
            assertThat(response.kafka()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should work with ApiResponse wrapper")
        void shouldWorkWithApiResponseWrapper() {
            // Given
            HealthCheckResponse healthData = new HealthCheckResponse("UP", "UP", "UP");

            // When
            ApiResponse<HealthCheckResponse> apiResponse = 
                    ApiResponse.success("Application is healthy", healthData);

            // Then
            assertThat(apiResponse.data()).isEqualTo(healthData);
            assertThat(apiResponse.data().database()).isEqualTo("UP");
            assertThat(apiResponse.data().redis()).isEqualTo("UP");
            assertThat(apiResponse.data().kafka()).isEqualTo("UP");
        }
    }
}