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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.util.DelayUtils;
import org.junit.jupiter.api.Test;

import static com.revinate.assertj.json.JsonPathAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateInstanceWithTargetAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_SAMPLE_APP_CREATE_WITH_TARGET = "app-with-target";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + BROKER_SAMPLE_APP_CREATE_WITH_TARGET,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter1=config1",
		"spring.cloud.appbroker.services[0].target.name=SpacePerServiceInstance"
	})
	void shouldCreateAppInTargetWhenAddingNewProperties() {
		// when a service instance is created
		createServiceInstance();
		Optional<ServiceInstanceSummary> serviceInstance = getServiceInstance();
		assertThat(serviceInstance).hasValueSatisfying(value ->
			assertThat(value.getLastOperation()).contains("completed"));

		// then a backing application is deployed in a space named as the service instance id
		String spaceName = serviceInstance.orElseThrow(RuntimeException::new).getId();
		Optional<ApplicationSummary> backingApplication =
			getApplicationSummaryByNameAndSpace(BROKER_SAMPLE_APP_CREATE_WITH_TARGET, spaceName);
		assertThat(backingApplication).hasValueSatisfying(app -> {
			assertThat(app.getRunningInstances()).isEqualTo(1);

			// and has its route with the service instance id appended to it
			assertThat(app.getUrls()).isNotEmpty();
			assertThat(app.getUrls().get(0)).startsWith(BROKER_SAMPLE_APP_CREATE_WITH_TARGET + "-" + spaceName);
		});

		// when the service instance is updated
		updateServiceInstance(Collections.singletonMap("parameter2", "config2"));

		getServiceInstanceMono()
			.filter(summary -> summary.getLastOperation().contains("completed"))
			.repeatWhenEmpty(DelayUtils.exponentialBackOff(Duration.ofSeconds(2), Duration.ofSeconds(15), Duration.ofMinutes(5)))
			.blockOptional();

		// then the service instance has the initial parameters
		DocumentContext json = getSpringAppJsonByNameAndSpace(BROKER_SAMPLE_APP_CREATE_WITH_TARGET, spaceName);
		assertThat(json).jsonPathAsString("$.parameter1").isEqualTo("config1");

		// when the service instance is deleted
		deleteServiceInstance();

		// then the space is deleted
		List<String> spaces = getSpaces();
		assertThat(spaces).doesNotContain(spaceName);
	}

}