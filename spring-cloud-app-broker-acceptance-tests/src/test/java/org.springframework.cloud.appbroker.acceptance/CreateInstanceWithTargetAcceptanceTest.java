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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithTargetAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_SAMPLE_APP_CREATE_WITH_TARGET = "app-with-target";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + BROKER_SAMPLE_APP_CREATE_WITH_TARGET,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
		"spring.cloud.appbroker.services[0].apps[0].target.name=SpacePerServiceInstance"
	})
	void shouldPushAppWithTargetWhenCreateServiceCalled() {
		// when a service instance is created
		createServiceInstance();
		Optional<ServiceInstanceSummary> serviceInstance = getServiceInstance();
		assertThat(serviceInstance).isNotEmpty();

		// then a backing application is deployed in a space named as the service instance id
		String serviceInstanceId = serviceInstance.orElseThrow(RuntimeException::new).getId();
		String spaceName = serviceInstanceId;
		Optional<ApplicationSummary> backingApplication =
			getApplicationSummaryByNameAndSpace(BROKER_SAMPLE_APP_CREATE_WITH_TARGET, spaceName);
		assertThat(backingApplication).isNotEmpty();

		// and has its route with the service instance id appended to it
		ApplicationSummary applicationSummary = backingApplication.orElseThrow(RuntimeException::new);
		assertThat(applicationSummary.getUrls()).isNotEmpty();
		assertThat(applicationSummary.getUrls().get(0)).startsWith(BROKER_SAMPLE_APP_CREATE_WITH_TARGET + "-" + spaceName);

		// when the service instance is deleted
		deleteServiceInstance();

		// then the space is deleted
		List<String> spaces = getSpaces();
		assertThat(spaces).doesNotContain(spaceName);
	}

}