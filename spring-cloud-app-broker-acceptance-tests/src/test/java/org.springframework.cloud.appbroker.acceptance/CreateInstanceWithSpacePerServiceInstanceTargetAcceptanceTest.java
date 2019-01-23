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

import java.util.List;
import java.util.Optional;

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithSpacePerServiceInstanceTargetAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME_1 = "app-create-space-per1";
	private static final String APP_NAME_2 = "app-create-space-per2";
	private static final String SI_NAME = "si-create-space-per";

	private static final String BACKING_SI_NAME = "backing-service-space-per-target";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME_1,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_NAME,

		"spring.cloud.appbroker.services[0].apps[1].name=" + APP_NAME_2,
		"spring.cloud.appbroker.services[0].apps[1].path=" + BACKING_APP_PATH,

		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,

		"spring.cloud.appbroker.services[0].target.name=SpacePerServiceInstance"
	})
	void deployAppsInTargetSpaceOnCreateService() {
		// when a service instance is created with targets
		createServiceInstance(SI_NAME);

		// then backing applications are deployed in a space named as the service instance id
		String spaceName = getServiceInstanceGuid(SI_NAME);

		Optional<ApplicationSummary> backingApplication1 = getApplicationSummary(APP_NAME_1, spaceName);
		assertThat(backingApplication1).hasValueSatisfying(app -> {
			assertThat(app.getRunningInstances()).isEqualTo(1);

			// and has its route with the service instance id appended to it
			assertThat(app.getUrls()).isNotEmpty();
			assertThat(app.getUrls().get(0)).startsWith(APP_NAME_1 + "-" + spaceName);
		});

		Optional<ApplicationSummary> backingApplication2 = getApplicationSummary(APP_NAME_2, spaceName);
		assertThat(backingApplication2).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the services are bound to it
		ServiceInstance serviceInstance1 = getServiceInstance(BACKING_SI_NAME, spaceName);
		assertThat(serviceInstance1.getApplications()).contains(APP_NAME_1);

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// then the space is deleted
		List<String> spaces = getSpaces();
		assertThat(spaces).doesNotContain(spaceName);
	}
}