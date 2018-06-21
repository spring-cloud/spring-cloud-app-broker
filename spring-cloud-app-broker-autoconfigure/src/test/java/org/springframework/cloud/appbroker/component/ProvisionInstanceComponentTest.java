/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.component;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

// TODO This should be in the App Broker core subproject
// NB: this can't be in the core subproject. the dependency graph for subprojects is:
//           -deployer
//           /      \
//       -core    -deployer-cloudfoundry
//         /
//   -autoconfigure
// this test requires full autoconfiguration, sample configuration, and a sample SpringBootApplication, so it's
// well beyond the scope of what can be in core. It might be best to have a separate
// -integration subproject to contain this test and the test infrastructure it depends on.

/**
 * This is a black box test to validate the end-to-end flow of deploying an application from a service broker request.
 * The black box test validates the flow has been performed and external contracts are satisfied but does not
 * rely on external dependencies (eg pushing an app to a real CF)
 * {@see https://github.com/spring-cloud-incubator/spring-cloud-app-broker/issues/4}
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = TestAppBrokerApplication.class,
	properties = {
		/**
		 * TODO we should get the jar remotely but see
		 * {@link org.springframework.cloud.appbroker.deployer.cloudfoundry.AbstractCloudFoundryReactiveAppDeployer.getApplication}
		 */
		"spring.cloud.app.broker.deploy.path=src/test/resources/demo.jar",
		"spring.cloud.app.broker.deploy.appName=helloworldapp",

		// TODO not hardcode, we should be stubbing this with fake data
		"spring.cloud.app.broker.cf.api=https://api.cf.somewhere.springapps.io/",
		"spring.cloud.app.broker.cf.username=admin",
		"spring.cloud.app.broker.cf.password=adminpass",
		"spring.cloud.app.broker.cf.defaultOrg=test",
		"spring.cloud.app.broker.cf.defaultSpace=development",
		"spring.cloud.app.broker.cf.skipSslValidation=true"

	})
class ProvisionInstanceComponentTest {

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
	@Disabled
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
		assertThat("The CF API endpoint was called with operation").isEqualTo("push");// which is the value from the deployer
	}
}
