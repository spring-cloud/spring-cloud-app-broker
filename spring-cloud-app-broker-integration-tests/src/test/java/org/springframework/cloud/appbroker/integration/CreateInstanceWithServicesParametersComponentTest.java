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

import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.integration.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithServicesParametersComponentTest.APP_NAME;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithServicesParametersComponentTest.BACKING_SERVICE_NAME;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithServicesParametersComponentTest.BACKING_SI_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
	"spring.cloud.appbroker.services[0].services[0].plan=standard",
	"spring.cloud.appbroker.services[0].services[0].parameters-transformers[0].name=ParameterMapping",
	"spring.cloud.appbroker.services[0].services[0].parameters-transformers[0].args.include=paramA,paramC"
})
class CreateInstanceWithServicesParametersComponentTest extends WiremockComponentTest {

	protected static final String APP_NAME = "app-services-param";

	protected static final String BACKING_SI_NAME = "my-db-service";

	protected static final String BACKING_SERVICE_NAME = "db-service";

	protected static final String BACKING_PLAN_NAME = "standard";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Test
	void pushAppWithBackingServicesParameters() {
		cloudControllerFixture.stubFindSpaceV3();
		cloudControllerFixture.stubAppDoesNotExist(APP_NAME);
		cloudControllerFixture.stubPushApp(APP_NAME);

		// given services are available in the marketplace
		cloudControllerFixture.stubServiceExists(BACKING_SERVICE_NAME, BACKING_PLAN_NAME);

		// will create with filtered parameters and bind the service instance
		HashMap<String, Object> expectedCreationParameters = new HashMap<>();
		expectedCreationParameters.put("paramA", "valueA");
		expectedCreationParameters.put("paramC", Collections.singletonMap("paramC1", "valueC1"));

		cloudControllerFixture.stubCreateServiceInstanceWithParameters(BACKING_SI_NAME, expectedCreationParameters);
		cloudControllerFixture.stubCreateServiceBinding(APP_NAME, BACKING_SI_NAME);
		cloudControllerFixture.stubServiceInstanceExists(BACKING_SI_NAME);

		// when a service instance is created with parameters
		HashMap<String, Object> creationParameters = new HashMap<>();
		creationParameters.put("paramA", "valueA");
		creationParameters.put("paramB", "valueB");
		creationParameters.put("paramC", Collections.singletonMap("paramC1", "valueC1"));

		given(brokerFixture.serviceInstanceRequest(creationParameters))
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
