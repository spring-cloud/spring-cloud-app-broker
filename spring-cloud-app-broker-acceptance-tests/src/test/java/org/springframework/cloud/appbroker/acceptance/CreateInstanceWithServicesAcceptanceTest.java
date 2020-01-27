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

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithServicesAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME = "app-create-services";

	private static final String SI_NAME = "si-create-services";

	private static final String BACKING_SI_1_NAME = "backing-service-instance-created";

	private static final String BACKING_SI_2_NAME = "backing-service-instance-existing";

	private static final String SUFFIX = "create-instance-with-services";

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
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_1_NAME,
		"spring.cloud.appbroker.services[0].apps[0].services[1].service-instance-name=" + BACKING_SI_2_NAME,
		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_1_NAME
	})
	void deployAppsAndCreateServicesOnCreateService() {
		// given that a service is available in the marketplace
		createServiceInstance(BACKING_SERVICE_NAME, PLAN_NAME, BACKING_SI_2_NAME, emptyMap());

		// when a service instance is created
		createServiceInstance(SI_NAME);

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummary(APP_NAME);
		assertThat(backingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the services are bound to it
		ServiceInstance serviceInstance1 = getServiceInstance(BACKING_SI_1_NAME);
		assertThat(serviceInstance1.getApplications()).contains(APP_NAME);

		ServiceInstance serviceInstance2 = getServiceInstance(BACKING_SI_2_NAME);
		assertThat(serviceInstance2.getApplications()).contains(APP_NAME);

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// and the backing services are deleted
		assertThat(listServiceInstances()).doesNotContain(BACKING_SI_1_NAME);
		assertThat(listServiceInstances()).doesNotContain(BACKING_SI_2_NAME);

		// TODO: another story to only remove the instances with service definition specified (https://github.com/spring-cloud/spring-cloud-app-broker/issues/316)
//		// service without specification has no applications bound to it
//		ServiceInstance serviceInstance2AfterDeletion = getServiceInstance(BACKING_SI_2_NAME);
//		assertThat(serviceInstance2AfterDeletion.getApplications()).isEmpty();
//
//		deleteServiceInstance(BACKING_SI_2_NAME);
	}

}
