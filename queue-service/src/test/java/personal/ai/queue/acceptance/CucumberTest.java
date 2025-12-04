package personal.ai.queue.acceptance;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

/**
 * Cucumber 테스트 실행을 위한 JUnit 5 Platform Suite
 * Cucumber JUnit Platform Engine을 사용하여 실행
 * agent.md Testing Strategy - BDD Style
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "personal.ai.queue.acceptance")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:build/reports/cucumber/queue-service.html, json:build/reports/cucumber/queue-service.json")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "")
public class CucumberTest {
}
