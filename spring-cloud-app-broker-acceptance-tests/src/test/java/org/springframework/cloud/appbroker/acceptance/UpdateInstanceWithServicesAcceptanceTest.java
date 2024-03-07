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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class UpdateInstanceWithServicesAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME = "app-update-services";

	private static final String SI_NAME = "si-update-services";

	private static final String BACKING_SI_NAME = "backing-service-instance-update";

	private static final String SUFFIX = "update-instance-with-services";

	private static final String APP_SERVICE_NAME = "app-service-" + SUFFIX;

	private static final String BACKING_SERVICE_NAME = "backing-service-" + SUFFIX;

	@Autowired
	private HealthListener healthListener;

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

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_NAME,

		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
		"spring.cloud.appbroker.services[0].services[0].rebind-on-update=true"
	})
	void shouldPushAppWithServicesBind() {
		// when a service instance is created
		createServiceInstance(SI_NAME);

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummary(APP_NAME);
		assertThat(backingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the services are bound to it
		ServiceInstance backingServiceInstance = getBackingServiceInstance(BACKING_SI_NAME);
		assertThat(backingServiceInstance.getApplications()).contains(APP_NAME);

		String path = backingApplication.get().getUrls().get(0);
		healthListener.start(path);

		// when the service instance is updated
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("parameter1", "value1");
		parameters.put("parameter2", "value2");
		parameters.put("parameter3", "value3");
		updateServiceInstance(SI_NAME, parameters);

		// then the backing application was updated with zero downtime
		healthListener.stop();
		assertThat(healthListener.getFailures()).isEqualTo(0);
		assertThat(healthListener.getSuccesses()).isGreaterThan(0);

		// then a backing application is re-deployed
		Optional<ApplicationSummary> updatedBackingApplication = getApplicationSummary(APP_NAME);
		assertThat(updatedBackingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the services are still bound to it
		ServiceInstance backingServiceInstanceUpdated = getBackingServiceInstance(BACKING_SI_NAME);
		assertThat(backingServiceInstanceUpdated.getApplications()).contains(APP_NAME);

		// then the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// and the backing service is deleted
		await().untilAsserted(() -> assertThat(listServiceInstances()).doesNotContain(BACKING_SI_NAME));
	}

}
