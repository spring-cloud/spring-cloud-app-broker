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
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithTargetAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_CREATE_WITH_TARGET = "app-create-with-target";
	private static final String APP_CREATE_WITH_TARGET_OTHER = "app-create-with-target-other";
	private static final String BACKING_SERVICE_INSTANCE = "backing-service-instance-target";
	private static final String BACKING_SERVICE_NAME = "backing-service-target";

	@BeforeEach
	void setUpServiceBrokerForService() {
		deployServiceBrokerForService(BACKING_SERVICE_NAME);
	}

	@AfterEach
	void tearDownServiceBrokerForService() {
		deleteServiceBrokerForService(BACKING_SERVICE_NAME);
	}

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_CREATE_WITH_TARGET,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SERVICE_INSTANCE,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SERVICE_INSTANCE,
		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=standard",

		"spring.cloud.appbroker.services[0].apps[1].name=" + APP_CREATE_WITH_TARGET_OTHER,
		"spring.cloud.appbroker.services[0].apps[1].path=classpath:demo.jar",

		"spring.cloud.appbroker.services[0].target.name=SpacePerServiceInstance"
	})
	void deployAppsInTargetSpaceOnCreateService() {
		// when a service instance is created with targets
		createServiceInstance();

		Optional<ServiceInstanceSummary> serviceInstance = getServiceInstance();
		assertThat(serviceInstance).hasValueSatisfying(value ->
			assertThat(value.getLastOperation()).contains("completed"));

		// then backing applications are deployed in a space named as the service instance id
		String spaceName = serviceInstance.orElseThrow(RuntimeException::new).getId();

		Optional<ApplicationSummary> backingApplication =
			getApplicationSummaryByNameAndSpace(APP_CREATE_WITH_TARGET, spaceName);
		assertThat(backingApplication).hasValueSatisfying(app -> {
			assertThat(app.getRunningInstances()).isEqualTo(1);

			// and has its route with the service instance id appended to it
			assertThat(app.getUrls()).isNotEmpty();
			assertThat(app.getUrls().get(0)).startsWith(APP_CREATE_WITH_TARGET + "-" + spaceName);
		});

		Optional<ApplicationSummary> backingApplicationOther =
			getApplicationSummaryByNameAndSpace(APP_CREATE_WITH_TARGET_OTHER, spaceName);
		assertThat(backingApplicationOther).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the services are bound to it
		Optional<ServiceInstanceSummary> serviceInstance1 = getServiceInstance(BACKING_SERVICE_INSTANCE, spaceName);
		assertThat(serviceInstance1).hasValueSatisfying(instance ->
			assertThat(instance.getApplications()).contains(APP_CREATE_WITH_TARGET));

		// when the service instance is deleted
		deleteServiceInstance();

		// then the space is deleted
		List<String> spaces = getSpaces();
		assertThat(spaces).doesNotContain(spaceName);
	}
}