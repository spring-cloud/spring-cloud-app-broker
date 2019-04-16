/*
 * Copyright 2016-2019 the original author or authors.
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.appbroker.integration.fixtures.CredHubStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.appbroker.integration.fixtures.TestBindingCredentialsProviderFixture;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static io.restassured.RestAssured.given;
import static org.springframework.cloud.appbroker.integration.CreateBindingWithCredHubComponentTest.APP_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,

	"spring.credhub.url=http://localhost:8888"
})
class CreateBindingWithCredHubComponentTest extends WiremockComponentTest {

	static final String APP_NAME = "create-binding-credhub";

	private static final String SERVICE_INSTANCE_ID = "instance-id";
	private static final String BINDING_ID = "binding-id";
	private static final String CREDENTIAL_NAME = "credentials-json";
	private static final String APP_ID = "app-id";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CredHubStubFixture credHubFixture;

	@Autowired
	private TestBindingCredentialsProviderFixture bindingFixture;

	@Value("${spring.application.name}")
	private String brokerAppName;

	@Value("${spring.cloud.openservicebroker.catalog.services[0].id}")
	String serviceDefinitionId;

	@Test
	void createAppBindingWithCredHub() {
		String credentialName = credHubFixture.bindingCredentialName(brokerAppName, serviceDefinitionId,
			BINDING_ID, CREDENTIAL_NAME);
		
		credHubFixture.stubWriteCredential(credentialName,
			matchingJsonPath("$.[?(@.value.credential1 == '" +
				bindingFixture.getCredentials().get("credential1") +
				"')]"),
			matchingJsonPath("$.[?(@.value.credential2 == '" +
				bindingFixture.getCredentials().get("credential2") +
				"')]"));

		credHubFixture.stubAddAppPermission(credentialName, "mtls-app:" + APP_ID);

		// when a service binding is created
		given(brokerFixture.serviceAppBindingRequest())
			.when()
			.put(brokerFixture.createBindingUrl(), SERVICE_INSTANCE_ID, BINDING_ID)
			.then()
			.statusCode(HttpStatus.CREATED.value());
	}

	@Test
	void createServiceKeyWithCredHub() {
		String credentialName = credHubFixture.bindingCredentialName(brokerAppName, serviceDefinitionId,
			BINDING_ID, CREDENTIAL_NAME);

		credHubFixture.stubWriteCredential(credentialName,
			matchingJsonPath("$.[?(@.value.credential1 == '" +
				bindingFixture.getCredentials().get("credential1") +
				"')]"),
			matchingJsonPath("$.[?(@.value.credential2 == '" +
				bindingFixture.getCredentials().get("credential2") +
				"')]"));

		credHubFixture.stubAddAppPermission(credentialName, "uaa-client:service-key-client-id");

		// when a service binding is created
		given(brokerFixture.serviceKeyRequest())
			.when()
			.put(brokerFixture.createBindingUrl(), SERVICE_INSTANCE_ID, BINDING_ID)
			.then()
			.statusCode(HttpStatus.CREATED.value());
	}

}