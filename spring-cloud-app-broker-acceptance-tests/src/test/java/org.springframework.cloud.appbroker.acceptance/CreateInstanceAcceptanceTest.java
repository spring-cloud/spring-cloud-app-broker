/*
 * Copyright 2016-2018. the original author or authors.
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

package org.springframework.cloud.appbroker.acceptance;

import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.junit.jupiter.api.Test;

import static com.revinate.assertj.json.JsonPathAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_SAMPLE_APP_CREATE_1 = "broker-app-create-1";
	private static final String BROKER_SAMPLE_APP_CREATE_2 = "broker-app-create-2";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + BROKER_SAMPLE_APP_CREATE_1,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
		
		"spring.cloud.appbroker.services[0].apps[0].environment.ENV_VAR_1=value1",
		"spring.cloud.appbroker.services[0].apps[0].environment.ENV_VAR_2=value2",
		"spring.cloud.appbroker.services[0].apps[0].properties.memory=2G",
		"spring.cloud.appbroker.services[0].apps[0].properties.count=2",

		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].name=SpringSecurityBasicAuth",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.length=14",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-uppercase-alpha=true",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-lowercase-alpha=true",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-numeric=false",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-special=false",

		"spring.cloud.appbroker.services[0].apps[1].name=" + BROKER_SAMPLE_APP_CREATE_2,
		"spring.cloud.appbroker.services[0].apps[1].path=classpath:demo.jar"
	})
	void shouldPushAppWhenCreateServiceCalled() {
		// when a service instance is created
		createServiceInstance();

		Optional<ServiceInstanceSummary> serviceInstance = getServiceInstance();
		assertThat(serviceInstance).hasValueSatisfying(value ->
			assertThat(value.getLastOperation()).contains("completed"));

		// then a backing applications are deployed
		Optional<ApplicationSummary> backingApplication1 = getApplicationSummaryByName(BROKER_SAMPLE_APP_CREATE_1);
		assertThat(backingApplication1).hasValueSatisfying(app -> {
			assertThat(app.getInstances()).isEqualTo(2);
			assertThat(app.getRunningInstances()).isEqualTo(2);
			assertThat(app.getMemoryLimit()).isEqualTo(2048);
		});

		Optional<ApplicationSummary> backingApplication2 = getApplicationSummaryByName(BROKER_SAMPLE_APP_CREATE_2);
		assertThat(backingApplication2).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and has the environment variables
		DocumentContext json = getSpringAppJsonByName(BROKER_SAMPLE_APP_CREATE_1);
		assertEnvironmentVariablesSet(json);
		assertBasicAuthCredentialsProvided(json);

		// when the service instance is deleted
		deleteServiceInstance();

		// then the backing applications are deleted
		Optional<ApplicationSummary> backingApplication1AfterDeletion = getApplicationSummaryByName(BROKER_SAMPLE_APP_CREATE_1);
		assertThat(backingApplication1AfterDeletion).isEmpty();

		Optional<ApplicationSummary> backingApplication2AfterDeletion = getApplicationSummaryByName(BROKER_SAMPLE_APP_CREATE_2);
		assertThat(backingApplication2AfterDeletion).isEmpty();
	}

	private void assertEnvironmentVariablesSet(DocumentContext json) {
		assertThat(json).jsonPathAsString("$.ENV_VAR_1").isEqualTo("value1");
		assertThat(json).jsonPathAsString("$.ENV_VAR_2").isEqualTo("value2");
	}

	private void assertBasicAuthCredentialsProvided(DocumentContext json) {
		assertThat(json).jsonPathAsString("$.spring.security.user.name")
			.matches("[a-zA-Z]{14}");
		assertThat(json).jsonPathAsString("$.spring.security.user.password")
			.matches("[a-zA-Z]{14}");
	}
}