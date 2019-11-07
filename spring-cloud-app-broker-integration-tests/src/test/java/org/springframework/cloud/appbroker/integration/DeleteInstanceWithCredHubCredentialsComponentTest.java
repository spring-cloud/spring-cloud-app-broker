/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.cloud.appbroker.integration.fixtures.CredHubStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.appbroker.integration.fixtures.UaaStubFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.DeleteInstanceWithCredHubCredentialsComponentTest.APP_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,

	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].name=SpringSecurityBasicAuth",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.length=14",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-uppercase-alpha=true",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-lowercase-alpha=true",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-numeric=false",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-special=false",

	"spring.cloud.appbroker.services[0].apps[0].credential-providers[1].name=SpringSecurityOAuth2",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[1].args.registration=example-app-client",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[1].args.client-id=test-client",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[1].args.grant-types=[\"client_credentials\"]",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[1].args.length=12",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[1].args.include-uppercase-alpha=true",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[1].args.include-lowercase-alpha=true",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[1].args.include-numeric=false",
	"spring.cloud.appbroker.services[0].apps[0].credential-providers[1].args.include-special=false",

	"spring.credhub.url=http://localhost:8888"
})
class DeleteInstanceWithCredHubCredentialsComponentTest extends WiremockComponentTest {

	protected static final String APP_NAME = "first-app";

	private static final String SERVICE_INSTANCE_ID = "instance-id";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Autowired
	private UaaStubFixture uaaFixture;

	@Autowired
	private CredHubStubFixture credHubFixture;

	@Test
	void deleteAppWithCredentials() {
		cloudControllerFixture.stubAppExists(APP_NAME);
		cloudControllerFixture.stubServiceBindingDoesNotExist(APP_NAME);
		cloudControllerFixture.stubDeleteApp(APP_NAME);

		uaaFixture.stubDeleteClient("test-client");

		credHubFixture.stubDeleteCredential(APP_NAME, SERVICE_INSTANCE_ID, "basic");
		credHubFixture.stubDeleteCredential(APP_NAME, SERVICE_INSTANCE_ID, "oauth2");

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
