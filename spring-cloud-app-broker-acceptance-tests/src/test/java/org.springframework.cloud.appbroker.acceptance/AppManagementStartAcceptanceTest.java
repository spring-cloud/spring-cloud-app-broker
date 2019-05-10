/*
 * Copyright 2002-2019 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.acceptance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AppManagementStartAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String SUFFIX = "app-management-start";

	private static final String APP_1 = "app-1-" + SUFFIX;
	private static final String APP_2 = "app-2" + SUFFIX;
	private static final String SI_NAME = "si-managed" + SUFFIX;

	private static final String APP_SERVICE_NAME = "app-service-"+ SUFFIX;
	private static final String BACKING_SERVICE_NAME = "backing-service-"+ SUFFIX;

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

	@BeforeEach
	void setUp() {
		StepVerifier.create(cloudFoundryService.deleteServiceInstance(SI_NAME))
					.verifyComplete();

		StepVerifier.create(cloudFoundryService.createServiceInstance(PLAN_NAME, APP_SERVICE_NAME, SI_NAME, null))
					.verifyComplete();

		StepVerifier.create(cloudFoundryService.getServiceInstance(SI_NAME))
					.assertNext(serviceInstance -> assertThat(serviceInstance.getStatus()).isEqualTo("succeeded"))
					.verifyComplete();
	}

	@AfterEach
	void cleanUp() {
		StepVerifier.create(cloudFoundryService.deleteServiceInstance(SI_NAME))
					.verifyComplete();

		StepVerifier.create(getApplications(APP_1, APP_2))
					.verifyError();
	}

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_1,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[1].name=" + APP_2,
		"spring.cloud.appbroker.services[0].apps[1].path=" + BACKING_APP_PATH
	})
	void startApps() {
		StepVerifier.create(cloudFoundryService.stopApplication(APP_1)
											   .then(cloudFoundryService.stopApplication(APP_2)))
					.verifyComplete();

		StepVerifier.create(getApplications(APP_1, APP_2))
					.assertNext(apps -> assertThat(apps).extracting("runningInstances").containsOnly(0))
					.verifyComplete();

		StepVerifier.create(manageApps(SI_NAME, "start"))
					.assertNext(result -> assertThat(result).contains("starting"))
					.verifyComplete();

		StepVerifier.create(getApplications(APP_1, APP_2))
					.assertNext(apps -> assertThat(apps).extracting("runningInstances").containsOnly(1))
					.verifyComplete();
	}
}