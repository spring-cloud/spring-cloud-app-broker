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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.junit.jupiter.api.Test;

import static com.revinate.assertj.json.JsonPathAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateInstanceWithTargetAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME = "app-update-target";
	private static final String SI_NAME = "si-update-target";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter1=config1",

		"spring.cloud.appbroker.services[0].target.name=SpacePerServiceInstance"
	})
	void deployAppsInTargetSpaceOnUpdateService() {
		// when a service instance is created
		createServiceInstance(SI_NAME);

		// then a backing application is deployed in a space named as the service instance id
		String spaceName = getServiceInstanceGuid(SI_NAME);

		Optional<ApplicationSummary> backingApplication = getApplicationSummary(APP_NAME, spaceName);
		assertThat(backingApplication).hasValueSatisfying(app -> {
			assertThat(app.getRunningInstances()).isEqualTo(1);

			// and has its route with the service instance id appended to it
			assertThat(app.getUrls()).isNotEmpty();
			assertThat(app.getUrls().get(0)).startsWith(APP_NAME + "-" + spaceName);
		});

		// when the service instance is updated
		updateServiceInstance(SI_NAME, Collections.singletonMap("parameter2", "config2"));

		// then the service instance has the initial parameters
		DocumentContext json = getSpringAppJson(APP_NAME, spaceName);
		assertThat(json).jsonPathAsString("$.parameter1").isEqualTo("config1");

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// then the space is deleted
		List<String> spaces = getSpaces();
		assertThat(spaces).doesNotContain(spaceName);
	}

}