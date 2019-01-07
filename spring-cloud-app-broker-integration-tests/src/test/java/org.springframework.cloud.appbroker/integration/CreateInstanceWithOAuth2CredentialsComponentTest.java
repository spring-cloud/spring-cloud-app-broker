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

package org.springframework.cloud.appbroker.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.integration.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.appbroker.integration.fixtures.UaaStubFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithOAuth2CredentialsComponentTest.APP_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].name=SpringSecurityOAuth2",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.registration=example-app-client",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.client-id=test-client",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.grant-types=[\"client_credentials\"]",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.length=14",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-uppercase-alpha=true",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-lowercase-alpha=true",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-numeric=false",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-special=false"
})
class CreateInstanceWithOAuth2CredentialsComponentTest extends WiremockComponentTest {

	static final String APP_NAME = "app-with-outh2-credentials";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Autowired
	private UaaStubFixture uaaFixture;
	
	@Test
	void pushAppWithOAuth2Credentials() {
		cloudControllerFixture.stubAppDoesNotExist(APP_NAME);
		cloudControllerFixture.stubPushApp(APP_NAME,
			matchingJsonPath("$.environment_json[?(@.SPRING_APPLICATION_JSON =~ " +
				"/.*spring.*security.*oauth2.*client.*registration.*example-app-client.*client-id.*:.*test-client.*/)]"),
			matchingJsonPath("$.environment_json[?(@.SPRING_APPLICATION_JSON =~ " +
				"/.*spring.*security.*oauth2.*client.*registration.*example-app-client.*client-secret.*:.*[a-zA-Z]{14}.*/)]"));

		uaaFixture.stubCreateClient();

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

	@Test
	void deleteAppWithOAuth2Credentials() {
		cloudControllerFixture.stubAppExists(APP_NAME);
		cloudControllerFixture.stubServiceBindingDoesNotExist(APP_NAME);
		cloudControllerFixture.stubDeleteApp(APP_NAME);

		uaaFixture.stubDeleteClient("test-client");

		// when the service instance is deleted
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.delete(brokerFixture.deleteServiceInstanceUrl(), "instance-id")
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