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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateInstanceAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME = "app-update";
	private static final String SI_NAME = "si-update";

	@Autowired
	private HealthListener healthListener;

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter1=config1",
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter2=config2",
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter3=config3",
		"spring.cloud.appbroker.services[0].apps[0].environment.parameter4=config4",
		"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[0].name=EnvironmentMapping",
		"spring.cloud.appbroker.services[0].apps[0].parameters-transformers[0].args.include=parameter1,parameter3"
	})
	void deployAppsOnUpdateService() {
		// given a service instance is created
		createServiceInstance(SI_NAME);

		// and a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummary(APP_NAME);
		assertThat(backingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		DocumentContext json = getSpringAppJson(APP_NAME);
		assertThat(json.read("$.parameter1").toString()).isEqualTo("config1");
		assertThat(json.read("$.parameter2").toString()).isEqualTo("config2");
		assertThat(json.read("$.parameter3").toString()).isEqualTo("config3");

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

		// the backing application is updated with the new parameters
		json = getSpringAppJson(APP_NAME);
		assertThat(json.read("$.parameter1").toString()).isEqualTo("value1");
		assertThat(json.read("$.parameter2").toString()).isEqualTo("config2");
		assertThat(json.read("$.parameter3").toString()).isEqualTo("value3");
		assertThat(json.read("$.parameter4").toString()).isEqualTo("config4");

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// then the backing application is deleted
		Optional<ApplicationSummary> backingApplicationAfterDeletion = getApplicationSummary(APP_NAME);
		assertThat(backingApplicationAfterDeletion).isEmpty();
	}
}