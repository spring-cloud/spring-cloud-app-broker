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

package org.springframework.cloud.appbroker.acceptance;

import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_CREATE_1 = "app-create-1";

	private static final String APP_CREATE_2 = "app-create-2";

	private static final String SI_NAME = "si-create";

	private static final String SUFFIX = "create-instance";

	private static final String APP_SERVICE_NAME = "app-service-" + SUFFIX;

	private static final String BACKING_SERVICE_NAME = "backing-service-" + SUFFIX;

	@Override
	protected String testSuffix() {
		return SUFFIX;
	}

	@Override
	protected String appServiceName() {
		return APP_SERVICE_NAME;
	}

	@Override
	protected String backingServiceName() {
		return BACKING_SERVICE_NAME;
	}

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_CREATE_1,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,

		"spring.cloud.appbroker.services[0].apps[0].environment.ENV_VAR_1=value1",
		"spring.cloud.appbroker.services[0].apps[0].environment.ENV_VAR_2=value2",
		"spring.cloud.appbroker.services[0].apps[0].properties.memory=2G",
		"spring.cloud.appbroker.services[0].apps[0].properties.count=2",

		"spring.cloud.appbroker.services[0].apps[1].name=" + APP_CREATE_2,
		"spring.cloud.appbroker.services[0].apps[1].path=" + BACKING_APP_PATH
	})
	void deployAppsOnCreateService() {
		// when a service instance is created
		createServiceInstance(SI_NAME);

		// then a backing applications are deployed
		Optional<ApplicationSummary> backingApplication1 = getApplicationSummary(APP_CREATE_1);
		assertThat(backingApplication1).hasValueSatisfying(app -> {
			assertThat(app.getInstances()).isEqualTo(2);
			assertThat(app.getRunningInstances()).isGreaterThanOrEqualTo(1);
			assertThat(app.getMemoryLimit()).isEqualTo(2048);
		});

		Optional<ApplicationSummary> backingApplication2 = getApplicationSummary(APP_CREATE_2);
		assertThat(backingApplication2).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and has the environment variables
		DocumentContext json = getSpringAppJson(APP_CREATE_1);
		assertEnvironmentVariablesSet(json);

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// then the backing applications are deleted
		Optional<ApplicationSummary> backingApplication1AfterDeletion = getApplicationSummary(APP_CREATE_1);
		assertThat(backingApplication1AfterDeletion).isEmpty();

		Optional<ApplicationSummary> backingApplication2AfterDeletion = getApplicationSummary(APP_CREATE_2);
		assertThat(backingApplication2AfterDeletion).isEmpty();
	}

	private void assertEnvironmentVariablesSet(DocumentContext json) {
		assertThat(json.read("$.ENV_VAR_1").toString()).isEqualTo("value1");
		assertThat(json.read("$.ENV_VAR_2").toString()).isEqualTo("value2");
	}

}
