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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.springframework.cloud.appbroker.integration.DeleteBindingWithCredHubComponentTest.APP_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,

	"spring.credhub.url=http://localhost:8888"
})
class DeleteBindingWithCredHubComponentTest extends WiremockComponentTest {

	static final String APP_NAME = "delete-binding-credhub";

	private static final String SERVICE_INSTANCE_ID = "instance-id";
	private static final String BINDING_ID = "binding-id";
	private static final String CREDENTIAL_NAME = "credentials-json";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CredHubStubFixture credHubFixture;

	@Value("${spring.application.name}")
	private String brokerAppName;

	@Value("${spring.cloud.openservicebroker.catalog.services[0].id}")
	String serviceDefinitionId;

	@Test
	void deleteAppBindingWithCredHub() {
		String credentialName = credHubFixture.bindingCredentialName(brokerAppName, serviceDefinitionId,
			BINDING_ID, CREDENTIAL_NAME);

		credHubFixture.stubFindCredential(credentialName);
		credHubFixture.stubDeleteCredential(credentialName);

		// when a service binding is deleted
		given(brokerFixture.serviceAppBindingRequest())
			.when()
			.delete(brokerFixture.deleteBindingUrl(), SERVICE_INSTANCE_ID, BINDING_ID)
			.then()
			.statusCode(HttpStatus.OK.value());
	}
}