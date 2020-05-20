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
import org.springframework.cloud.appbroker.integration.fixtures.UaaStubFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithOAuth2CredentialsComponentTest.APP_NAME_1;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithOAuth2CredentialsComponentTest.APP_NAME_2;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithOAuth2CredentialsComponentTest.PLAN_NAME;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithOAuth2CredentialsComponentTest.SERVICE_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=" + SERVICE_NAME,
	"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,

	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME_1,
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].name=SpringSecurityOAuth2",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.registration=example-app-client",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.client-id=test-client1",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.grant-types=[\"client_credentials\"]",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.length=14",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-uppercase-alpha=true",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-lowercase-alpha=true",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-numeric=false",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-special=false",

	"spring.cloud.appbroker.services[0].apps[1].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[1].name=" + APP_NAME_2,
	"spring.cloud.appbroker.services[0].apps[1].credential-providers[0].name=SpringSecurityOAuth2",
	"spring.cloud.appbroker.services[0].apps[1].credential-providers[0].args.registration=example-app-client",
	"spring.cloud.appbroker.services[0].apps[1].credential-providers[0].args.client-id=test-client2",
	"spring.cloud.appbroker.services[0].apps[1].credential-providers[0].args.grant-types=[\"client_credentials\"]",
	"spring.cloud.appbroker.services[0].apps[1].credential-providers[0].args.length=14",
	"spring.cloud.appbroker.services[0].apps[1].credential-providers[0].args.include-uppercase-alpha=true",
	"spring.cloud.appbroker.services[0].apps[1].credential-providers[0].args.include-lowercase-alpha=true",
	"spring.cloud.appbroker.services[0].apps[1].credential-providers[0].args.include-numeric=false",
	"spring.cloud.appbroker.services[0].apps[1].credential-providers[0].args.include-special=false",
	"spring.cloud.appbroker.services[0].apps[1].properties.use-spring-application-json=false"
})
class CreateInstanceWithOAuth2CredentialsComponentTest extends WiremockComponentTest {

	protected static final String APP_NAME_1 = "app-with-outh2-credentials1";

	protected static final String APP_NAME_2 = "app-with-outh2-credentials2";

	protected static final String SERVICE_NAME = "example";

	protected static final String PLAN_NAME = "standard";

	private static final String SERVICE_INSTANCE_ID = "instance-id";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Autowired
	private UaaStubFixture uaaFixture;

	@Test
	void pushAppWithOAuth2Credentials() {
		cloudControllerFixture.stubAppDoesNotExist(APP_NAME_1);
		cloudControllerFixture.stubPushApp(APP_NAME_1,
			matchingJsonPath("$.environment_json[?(@.SPRING_APPLICATION_JSON =~ " +
				"/.*spring.security.oauth2.client.registration.example-app-client.client-id.*:.*test-client1.*/)]"),
			matchingJsonPath("$.environment_json[?(@.SPRING_APPLICATION_JSON =~ " +
				"/.*spring.security.oauth2.client.registration.example-app-client.client-secret.*:.*[a-zA-Z]{14}.*/)]"));

		cloudControllerFixture.stubAppDoesNotExist(APP_NAME_2);
		cloudControllerFixture.stubPushApp(APP_NAME_2,
			matchingJsonPath(
				"$.environment_json[?(@.['spring.security.oauth2.client.registration.example-app-client.client-id'] =~ " +
					"/test-client2/)]"),
			matchingJsonPath(
				"$.environment_json[?(@.['spring.security.oauth2.client.registration.example-app-client.client-secret'] =~ " +
					"/[a-zA-Z]{14}/)]"));

		uaaFixture.stubCreateClient("test-client1");
		uaaFixture.stubCreateClient("test-client2");

		// when a service instance is created
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), SERVICE_INSTANCE_ID)
			.then()
			.statusCode(HttpStatus.ACCEPTED.value());

		// when the "last_operation" API is polled
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.get(brokerFixture.getLastInstanceOperationUrl(), SERVICE_INSTANCE_ID)
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("state", is(equalTo(OperationState.IN_PROGRESS.toString())));

		String state = brokerFixture.waitForAsyncOperationComplete(SERVICE_INSTANCE_ID);
		assertThat(state).isEqualTo(OperationState.SUCCEEDED.toString());
	}

	@Test
	void deleteAppWithOAuth2Credentials() {
		cloudControllerFixture.stubAppExists(APP_NAME_1);
		cloudControllerFixture.stubServiceBindingDoesNotExist(APP_NAME_1);
		cloudControllerFixture.stubDeleteApp(APP_NAME_1);

		cloudControllerFixture.stubAppExists(APP_NAME_2);
		cloudControllerFixture.stubServiceBindingDoesNotExist(APP_NAME_2);
		cloudControllerFixture.stubDeleteApp(APP_NAME_2);

		uaaFixture.stubDeleteClient("test-client1");
		uaaFixture.stubDeleteClient("test-client2");

		// when the service instance is deleted
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.delete(brokerFixture.deleteServiceInstanceUrl(), SERVICE_INSTANCE_ID)
			.then()
			.statusCode(HttpStatus.ACCEPTED.value());

		// when the "last_operation" API is polled
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.get(brokerFixture.getLastInstanceOperationUrl(), SERVICE_INSTANCE_ID)
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("state", is(equalTo(OperationState.IN_PROGRESS.toString())));

		String state = brokerFixture.waitForAsyncOperationComplete(SERVICE_INSTANCE_ID);
		assertThat(state).isEqualTo(OperationState.SUCCEEDED.toString());
	}

}
