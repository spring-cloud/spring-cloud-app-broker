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

import java.util.Collections;
import java.util.Optional;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithParametersAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_SAMPLE_APP_CREATE = "broker-sample-app-create-with-parameters";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + BROKER_SAMPLE_APP_CREATE,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	})
	void shouldPushAppWhenCreateServiceCalled() {
		// when a service instance is created
		createServiceInstanceWithParameters(Collections.singletonMap("ENV_VAR_1", "value1"));

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummaryByName(BROKER_SAMPLE_APP_CREATE);
		assertThat(backingApplication).isNotEmpty();

		// and has the environment variables
		ApplicationEnvironments applicationEnvironments = getApplicationEnvironmentByName(BROKER_SAMPLE_APP_CREATE);
		assertThat(applicationEnvironments.getUserProvided().get("SPRING_APPLICATION_JSON")).asString()
			.contains("\"ENV_VAR_1\":\"value1\"");
	}

}