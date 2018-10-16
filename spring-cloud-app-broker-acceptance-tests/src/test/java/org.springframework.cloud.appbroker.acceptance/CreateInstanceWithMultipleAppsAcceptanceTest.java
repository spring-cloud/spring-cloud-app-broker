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

import java.util.Optional;

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithMultipleAppsAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_SAMPLE_APP_CREATE_1 = "broker-app-create-1";
	private static final String BROKER_SAMPLE_APP_CREATE_2 = "broker-app-create-2";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + BROKER_SAMPLE_APP_CREATE_1,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
		"spring.cloud.appbroker.services[0].apps[1].name=" + BROKER_SAMPLE_APP_CREATE_2,
		"spring.cloud.appbroker.services[0].apps[1].path=classpath:demo.jar",
	})
	void shouldPushMultipleAppsWhenCreateServiceCalled() {
		// when a service instance is created
		createServiceInstance();

		Optional<ServiceInstanceSummary> serviceInstance = getServiceInstance();
		assertThat(serviceInstance).isNotEmpty();

		// then the backing applications are deployed
		Optional<ApplicationSummary> backingApplication1 = getApplicationSummaryByName(BROKER_SAMPLE_APP_CREATE_1);
		assertThat(backingApplication1).isNotEmpty();
		Optional<ApplicationSummary> backingApplication2 = getApplicationSummaryByName(BROKER_SAMPLE_APP_CREATE_2);
		assertThat(backingApplication2).isNotEmpty();

		// when the service instance is deleted
		deleteServiceInstance();

		// then the backing applications are deleted
		Optional<ApplicationSummary> backingApplication1AfterDelete = getApplicationSummaryByName(BROKER_SAMPLE_APP_CREATE_1);
		assertThat(backingApplication1AfterDelete).isEmpty();
		Optional<ApplicationSummary> backingApplication2AfterDelete = getApplicationSummaryByName(BROKER_SAMPLE_APP_CREATE_2);
		assertThat(backingApplication2AfterDelete).isEmpty();
	}

}