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

import java.util.List;
import java.util.Optional;

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithServicesSpacePerServiceInstanceTargetAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME = "app-create-two-space-per";
	private static final String SI_NAME = "si-create-two-space-per";
	private static final String BACKING_SI_NAME_1 = "backing-service-two-space-per-target-1";
	private static final String BACKING_SI_NAME_2 = "backing-service-two-space-per-target-2";
	private static final String SUFFIX = "two-space-per-si";
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
		"spring.cloud.appbroker.services[0].target.name=SpacePerServiceInstance",

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_NAME_1,
		"spring.cloud.appbroker.services[0].apps[0].services[1].service-instance-name=" + BACKING_SI_NAME_2,

		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME_1,

		"spring.cloud.appbroker.services[0].services[1].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[1].plan=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].services[1].service-instance-name=" + BACKING_SI_NAME_2
	})
	void deployAppsInTargetSpaceOnCreateService() {
		// when a service instance is created with targets
		createServiceInstance(SI_NAME);

		// then backing applications are deployed in a space named as the service instance id
		String spaceName = getServiceInstanceGuid(SI_NAME);

		Optional<ApplicationSummary> backingApplication1 = getApplicationSummary(APP_NAME, spaceName);
		assertThat(backingApplication1).hasValueSatisfying(app -> {
			assertThat(app.getRunningInstances()).isEqualTo(1);

			// and has its route with the service instance id appended to it
			assertThat(app.getUrls()).isNotEmpty();
			assertThat(app.getUrls().get(0)).startsWith(APP_NAME + "-" + spaceName);
		});

		// and the services are bound to it
		ServiceInstance serviceInstance1 = getBackingServiceInstance(BACKING_SI_NAME_1, spaceName);
		assertThat(serviceInstance1.getApplications()).contains(APP_NAME);
		ServiceInstance serviceInstance2 = getBackingServiceInstance(BACKING_SI_NAME_2, spaceName);
		assertThat(serviceInstance2.getApplications()).contains(APP_NAME);

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// then the space is deleted
		List<String> spaces = getSpaces();
		assertThat(spaces).doesNotContain(spaceName);
	}

}
