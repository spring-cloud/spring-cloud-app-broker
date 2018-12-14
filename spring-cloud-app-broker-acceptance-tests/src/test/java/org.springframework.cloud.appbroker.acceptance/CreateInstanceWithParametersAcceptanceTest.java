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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.junit.jupiter.api.Test;

import static com.revinate.assertj.json.JsonPathAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithParametersAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME = "app-create-params";
	private static final String SI_NAME = "si-create-params";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter1=config1",
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter2=config2",
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter3=config3",

		"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[0].name=EnvironmentMapping",
		"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[0].args.include=parameter1,parameter3",

		"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[1].name=PropertyMapping",
		"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[1].args.include=memory",
	})
	void deployAppsWithParametersOnCreateService() {
		// when a service instance is created
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("parameter1", "value1");
		parameters.put("parameter2", "value2");
		parameters.put("parameter3", "value3");
		parameters.put("count", 5);
		parameters.put("memory", "2G");

		createServiceInstance(SI_NAME, parameters);

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummary(APP_NAME);
		assertThat(backingApplication).hasValueSatisfying(app -> {
			assertThat(app.getInstances()).isEqualTo(1);
			assertThat(app.getRunningInstances()).isEqualTo(1);
			assertThat(app.getMemoryLimit()).isEqualTo(2048);
		});

		// and has the environment variables
		DocumentContext json = getSpringAppJson(APP_NAME);
		assertThat(json).jsonPathAsString("$.parameter1").isEqualTo("value1");
		assertThat(json).jsonPathAsString("$.parameter2").isEqualTo("config2");
		assertThat(json).jsonPathAsString("$.parameter3").isEqualTo("value3");

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);
	}

}