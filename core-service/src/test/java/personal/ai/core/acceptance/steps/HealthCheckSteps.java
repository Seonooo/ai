package personal.ai.core.acceptance.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Health Check Feature Step Definitions
 * agent.md Testing Strategy - BDD Style (Given-When-Then)
 */
public class HealthCheckSteps {

    @Autowired
    @LocalServerPort
    private int port;

    private Response response;

    @Given("Core 서비스가 정상 동작 중이다")
    public void coreServiceIsRunning() {
        RestAssured.port = port;
    }

    @When("헬스 체크 API를 호출하면")
    public void callHealthCheckApi() {
        response = RestAssured
                .given()
                .contentType("application/json")
                .when()
                .get("/api/v1/health");
    }

    @Then("응답 상태 코드는 {int}이다")
    public void responseStatusCodeIs(int statusCode) {
        assertThat(response.statusCode()).isEqualTo(statusCode);
    }

    @And("응답 결과는 {string}이다")
    public void responseResultIs(String result) {
        assertThat(response.jsonPath().getString("result")).isEqualTo(result);
    }

    @And("데이터베이스 상태 정보가 포함되어 있다")
    public void databaseStatusIsIncluded() {
        assertThat(response.jsonPath().getString("data.database")).isNotNull();
    }

    @And("Redis 상태 정보가 포함되어 있다")
    public void redisStatusIsIncluded() {
        assertThat(response.jsonPath().getString("data.redis")).isNotNull();
    }

    @And("Kafka 상태 정보가 포함되어 있다")
    public void kafkaStatusIsIncluded() {
        assertThat(response.jsonPath().getString("data.kafka")).isNotNull();
    }
}
