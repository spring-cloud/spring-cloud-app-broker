/*
 * Copyright 2002-2020 the original author or authors
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

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingRecentAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_CREATE_1 = "app-logging-recent-1";

	private static final String SI_NAME = "si-logging-recent";

	private static final String SUFFIX = "logging-recent-instance";

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
		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_CREATE_1,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].target.name=SpacePerServiceInstance",
		"spring.cloud.appbroker.deployer.cloudfoundry.properties.stack=cflinuxfs4"
	})
	void shouldReturnBackingApplicationLogs() {
		createServiceInstance(SI_NAME);

		String lines = callCLICommand(
			List.of("cf", "service-logs", SI_NAME, "--recent", "--skip-ssl-validation"))
			.block(Duration.ofSeconds(35));

		assertThat(lines).isNotEmpty();

		assertThat(lines).contains("Created app with guid");
		assertThat(lines).contains("Updated app with guid");
		assertThat(lines).contains("APP/PROC/WEB");
	}

	@AfterEach
	void tearDown() {
		deleteServiceInstance(SI_NAME);
	}

}
