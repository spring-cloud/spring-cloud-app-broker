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

import java.util.HashMap;
import java.util.Map;

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
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithParametersMappingComponentTest.APP_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.services[0].apps[0].environment.parameter1=config1",
	"spring.cloud.appbroker.services[0].apps[0].environment.parameter2=false",
	"spring.cloud.appbroker.services[0].apps[0].environment.parameter3=config3",
	"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[0].name=EnvironmentMapping",
	"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[0].args.include=parameter1,parameter2",
	"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[1].name=PropertyMapping",
	"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[1].args.include=count,memory"
})
class CreateInstanceWithParametersMappingComponentTest extends WiremockComponentTest {

	protected static final String APP_NAME = "app-with-env-create-params";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Test
	void pushAppWithParametersTransformedToEnvironmentVariables() {
		cloudControllerFixture.stubFindSpaceV3();
		cloudControllerFixture.stubAppDoesNotExist(APP_NAME);
		cloudControllerFixture.stubPushApp(APP_NAME,
			matchingJsonPath("$.environment_json[?(@.SPRING_APPLICATION_JSON =~ /.*parameter1.*:.*value1.*/)]"),
			matchingJsonPath("$.environment_json[?(@.SPRING_APPLICATION_JSON =~ /.*parameter2.*:.*true.*/)]"),
			matchingJsonPath("$.environment_json[?(@.SPRING_APPLICATION_JSON =~ /.*parameter3.*:.*config3.*/)]"),
			matchingJsonPath("$.[?(@.instances == '2')]"),
			matchingJsonPath("$.[?(@.memory == '2048')]"));

		// given a set of parameters
		Map<String, Object> params = new HashMap<>();
		params.put("parameter1", "value1");
		params.put("parameter2", true);
		params.put("parameter3", "value3");
		params.put("count", 2);
		params.put("memory", "2G");

		// when a service instance is created
		given(brokerFixture.serviceInstanceRequest(params))
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
