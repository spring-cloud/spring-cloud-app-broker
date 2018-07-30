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
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static reactor.util.function.Tuples.of;

class CreateInstanceAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_SAMPLE_APP_CREATE = "broker-sample-app-create";

	@BeforeEach
	void setUp() {
		initializeBroker(newArrayList(
			of("spring.cloud.appbroker.apps[0].name", BROKER_SAMPLE_APP_CREATE),
			of("spring.cloud.appbroker.apps[0].path", "classpath:demo.jar"),
			of("spring.cloud.appbroker.apps[0].environment.ENV_VAR_1", "value1"),
			of("spring.cloud.appbroker.apps[0].environment.ENV_VAR_2", "value2"),
			of("spring.cloud.appbroker.apps[0].properties.spring.cloud.deployer.memory", "2G"),
			of("spring.cloud.appbroker.apps[0].properties.spring.cloud.deployer.count", "2")
			)
		);
	}

	@Test
	void shouldPushAppWhenCreateServiceCalled() {
		// when a service instance is created
		createServiceInstance();

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummaryByName(BROKER_SAMPLE_APP_CREATE);
		assertThat(backingApplication).isNotEmpty();

		// and has the properties
		ApplicationSummary applicationSummary = backingApplication.orElseThrow(RuntimeException::new);
		assertThat(applicationSummary.getMemoryLimit()).isEqualTo(2048);
		assertThat(applicationSummary.getInstances()).isEqualTo(2);

		// and has the environment variables
		ApplicationEnvironments applicationEnvironments = getApplicationEnvironmentByName(BROKER_SAMPLE_APP_CREATE);
		assertThat(applicationEnvironments.getUserProvided().get("SPRING_APPLICATION_JSON")).asString()
			.contains("\"ENV_VAR_1\":\"value1\"");
		assertThat(applicationEnvironments.getUserProvided().get("SPRING_APPLICATION_JSON")).asString()
			.contains("\"ENV_VAR_2\":\"value2\"");
	}

}