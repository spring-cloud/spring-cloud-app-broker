/*
 * Copyright 2016-2018 the original author or authors.
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

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateInstanceWithServicesAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME = "app-update-services";
	private static final String SI_NAME = "si-update-services";

	private static final String BACKING_SERVICE_NAME = "backing-service-update";
	private static final String BACKING_SI_NAME = "backing-service-instance-update";

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
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",

		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_NAME,

		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=standard",
	})
	void shouldPushAppWithServicesBind() {
		// when a service instance is created
		createServiceInstance(SI_NAME);

		Optional<ServiceInstanceSummary> serviceInstance = getServiceInstance(SI_NAME);
		assertThat(serviceInstance).hasValueSatisfying(value ->
			assertThat(value.getLastOperation()).contains("completed"));

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummaryByName(APP_NAME);
		assertThat(backingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the services are bound to it
		Optional<ServiceInstanceSummary> backingServiceInstance = getServiceInstance(BACKING_SI_NAME);
		assertThat(backingServiceInstance).hasValueSatisfying(instance ->
			assertThat(instance.getApplications()).contains(APP_NAME));

		// when the service instance is updated
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("parameter1", "value1");
		parameters.put("parameter2", "value2");
		parameters.put("parameter3", "value3");
		updateServiceInstance(SI_NAME, parameters);

		Optional<ServiceInstanceSummary> updatedServiceInstance = getServiceInstance(SI_NAME);
		assertThat(updatedServiceInstance).hasValueSatisfying(value ->
			assertThat(value.getLastOperation()).contains("completed"));

		// then a backing application is re-deployed
		Optional<ApplicationSummary> updatedBackingApplication = getApplicationSummaryByName(APP_NAME);
		assertThat(updatedBackingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the services are still bound to it
		Optional<ServiceInstanceSummary> backingServiceInstanceUpdated = getServiceInstance(BACKING_SI_NAME);
		assertThat(backingServiceInstanceUpdated).hasValueSatisfying(instance ->
			assertThat(instance.getApplications()).contains(APP_NAME));

		// then the service instance is deleted
		deleteServiceInstance(SI_NAME);
	}
}
