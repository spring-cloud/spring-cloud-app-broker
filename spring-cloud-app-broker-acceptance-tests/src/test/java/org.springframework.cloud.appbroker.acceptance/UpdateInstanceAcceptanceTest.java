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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.util.DelayUtils;
import org.junit.jupiter.api.Test;

import static com.revinate.assertj.json.JsonPathAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class UpdateInstanceAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BROKER_SAMPLE_APP_UPDATE = "app-update";

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=example",
		"spring.cloud.appbroker.services[0].plan-name=standard",
		"spring.cloud.appbroker.services[0].apps[0].name=" + BROKER_SAMPLE_APP_UPDATE,
		"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter1=config1",
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter2=config2",
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter3=config3",
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter4=config4",
		"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[0].name=EnvironmentMapping",
		"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[0].args.include=parameter1,parameter3"
	})
	void shouldUpdateAppWhenUpdateServiceCalled() {
		// given a service instance is created
		createServiceInstance();

		// and a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummaryByName(BROKER_SAMPLE_APP_UPDATE);
		assertThat(backingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		DocumentContext json = getSpringAppJsonByName(BROKER_SAMPLE_APP_UPDATE);
		assertThat(json).jsonPathAsString("$.parameter1").isEqualTo("config1");
		assertThat(json).jsonPathAsString("$.parameter2").isEqualTo("config2");
		assertThat(json).jsonPathAsString("$.parameter3").isEqualTo("config3");

		// when the service instance is updated
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("parameter1", "value1");
		parameters.put("parameter2", "value2");
		parameters.put("parameter3", "value3");
		updateServiceInstance(parameters);

		Optional<ServiceInstanceSummary> serviceInstanceSummary = getServiceInstanceMono()
			.filter(summary -> summary.getLastOperation().contains("completed"))
			.repeatWhenEmpty(DelayUtils.exponentialBackOff(Duration.ofSeconds(2), Duration.ofSeconds(15), Duration.ofMinutes(5)))
			.blockOptional();
		assertThat(serviceInstanceSummary).isNotEmpty();

		// the backing application is updated with the new parameters
		json = getSpringAppJsonByName(BROKER_SAMPLE_APP_UPDATE);
		assertThat(json).jsonPathAsString("$.parameter1").isEqualTo("value1");
		assertThat(json).jsonPathAsString("$.parameter2").isEqualTo("config2");
		assertThat(json).jsonPathAsString("$.parameter3").isEqualTo("value3");
		assertThat(json).jsonPathAsString("$.parameter4").isEqualTo("config4");

		// when the service instance is deleted
		deleteServiceInstance();

		// then the backing application is deleted
		Optional<ApplicationSummary> backingApplicationAfterDeletion = getApplicationSummaryByName(BROKER_SAMPLE_APP_UPDATE);
		assertThat(backingApplicationAfterDeletion).isEmpty();
	}
}