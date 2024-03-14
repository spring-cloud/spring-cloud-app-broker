/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.integration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.integration.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithPropertiesComponentTest.APP_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.deployer.cloudfoundry.properties.memory=1G",
	"spring.cloud.appbroker.deployer.cloudfoundry.properties.count=1",
	"spring.cloud.appbroker.deployer.cloudfoundry.properties.health-check=http",
	"spring.cloud.appbroker.deployer.cloudfoundry.properties.health-check-http-endpoint=/myhealth",
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.services[0].apps[0].properties.memory=2G",
	"spring.cloud.appbroker.services[0].apps[0].properties.count=2",
	"spring.cloud.appbroker.services[0].apps[0].properties.health-check-timeout=180"
})
class CreateInstanceWithPropertiesComponentTest extends WiremockComponentTest {

	protected static final String APP_NAME = "app-with-properties";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Test
	void pushAppWithProperties() {
		cloudControllerFixture.stubFindSpaceV3();
		cloudControllerFixture.stubAppDoesNotExist(APP_NAME);
		cloudControllerFixture.stubPushApp(APP_NAME,
			matchingJsonPath("$.[?(@.memory == '2048')]"),
			matchingJsonPath("$.[?(@.instances == '2')]"),
			matchingJsonPath("$.[?(@.health_check_timeout == '180')]"),
			matchingJsonPath("$.[?(@.health_check_type == 'http')]"),
			matchingJsonPath("$.[?(@.health_check_http_endpoint == '/myhealth')]"));

		// when a service instance is created
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "instance-id")
			.then()
			.statusCode(HttpStatus.ACCEPTED.value());

		// when the "last_operation" API is polled
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.get(brokerFixture.getLastInstanceOperationUrl(), "instance-id")
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("state", is(equalTo(OperationState.IN_PROGRESS.toString())));

		String state = brokerFixture.waitForAsyncOperationComplete("instance-id");
		assertThat(state).isEqualTo(OperationState.SUCCEEDED.toString());
	}

}
