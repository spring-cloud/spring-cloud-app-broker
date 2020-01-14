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

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithServiceInstanceGuidSuffixTargetAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String SUFFIX = "create-si-guid";

	private static final String APP_NAME_1 = "app-create-" + SUFFIX;

	private static final String SI_NAME = "si-create-" + SUFFIX;

	private static final String BACKING_SI_NAME = "backing-si";

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
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME_1,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_NAME,
		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
		"spring.cloud.appbroker.services[0].target.name=ServiceInstanceGuidSuffix"
	})
	void deployAppsWithServiceInstanceGuidSuffixOnCreateService() {
		// when a service instance is created with target
		createServiceInstance(SI_NAME);

		// then backing application is created
		final String serviceInstanceGuid = getServiceInstanceGuid(SI_NAME);

		// then backing application is named as the concatenation of the name and it's service instance id
		final String expectedApplicationName = APP_NAME_1 + "-" + serviceInstanceGuid;
		Optional<ApplicationSummary> backingApplication = getApplicationSummary(expectedApplicationName);
		assertThat(backingApplication).hasValueSatisfying(app -> {
			assertThat(app.getName()).isEqualTo(expectedApplicationName);
			assertThat(app.getRunningInstances()).isEqualTo(1);
		});

		// and the services are bound to it
		final String expectedServiceInstanceName = BACKING_SI_NAME + "-" + serviceInstanceGuid;
		ServiceInstance serviceInstance1 = getServiceInstance(expectedServiceInstanceName);
		assertThat(serviceInstance1.getApplications()).contains(expectedApplicationName);

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);
	}

}
