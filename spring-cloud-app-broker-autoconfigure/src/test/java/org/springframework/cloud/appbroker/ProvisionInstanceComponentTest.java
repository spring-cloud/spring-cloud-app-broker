package org.springframework.cloud.appbroker;

import io.restassured.http.ContentType;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

//TODO This should be in the App Broker core subproject

/**
 * This is a black box test to validate the end-to-end flow of provisioning a Cf application from a service broker request
 * The black box test validates the flow has been performed and external contracts are satisfied but does not
 * rely on external dependencies (eg pushing an app to a real CF)
 * {@see https://github.com/spring-cloud-incubator/spring-cloud-app-broker/issues/4}
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = AppBrokerAutoConfigureApplication.class,
	properties = "spring.cloud.app.broker.create.instance.appName=helloworldapp")
public class ProvisionInstanceComponentTest {

	private String baseUrl = "";
	@Value("${local.server.port}")
	private String port;

	private static String createDefaultBody() {
		final String serviceId = "bdb1be2e-360b-495c-8115-d7697f9c6a9e";
		final String planId = "b973fb78-82f3-49ef-9b8b-c1876974a6cd";
		final String orgId = "org-guid-here";
		final String spaceId = "space-guid-here";
		return "{\n" +
			"  \"service_id\": \"" + serviceId + "\",\n" +
			"  \"plan_id\": \"" + planId + "\",\n" +
			"  \"organization_guid\": \"" + orgId + "\",\n" +
			"  \"space_guid\": \"" + spaceId + "\"\n" +
			"}\n";
	}

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port;
	}

	@Test
	void shouldPushAppWhenCreateServiceEndpointCalled() {
		final String instanceId = "123";
		given()
			.contentType(ContentType.JSON)
			.body(createDefaultBody())
			.put(baseUrl + "/v2/service_instances/{instance_id}", instanceId)
			.then()
			.statusCode(HttpStatus.CREATED.value());


		// deployer app object
		// TODO assert cloudfoundry API contract for pushing our helloworld application was satisfied
		Assert.assertThat("The CF API endpoint was called with operation", is(equalTo("push")));// which is the value from the deployer
	}
}
