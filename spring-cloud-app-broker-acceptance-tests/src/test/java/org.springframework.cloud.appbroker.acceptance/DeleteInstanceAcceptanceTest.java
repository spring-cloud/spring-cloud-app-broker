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
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

class DeleteInstanceAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_SAMPLE_APP_DELETE = "broker-sample-app-delete";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + BROKER_SAMPLE_APP_DELETE,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar"
	})
	void shouldDeleteAppsWhenDeleteServiceCalled() {
		// given a service instance is created
		createServiceInstance();

		// and a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummaryByName(BROKER_SAMPLE_APP_DELETE);
		assertThat(backingApplication).isNotEmpty();

		// when the service instance is deleted
		deleteServiceInstance();

		// then the backing application is deleted
		Optional<ApplicationSummary> backingApplicationAfterDeletion = getApplicationSummaryByName(BROKER_SAMPLE_APP_DELETE);
		assertThat(backingApplicationAfterDeletion).isEmpty();
	}
}