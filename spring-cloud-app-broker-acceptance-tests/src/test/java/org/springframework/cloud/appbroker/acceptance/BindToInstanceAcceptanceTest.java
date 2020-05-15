/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.acceptance;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BindToInstanceAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String BACKING_APP_NAME = "backing-app-binding";

	private static final String BOUND_APP_NAME = "bound-app";

	private static final String SI_NAME = "si-binding";

	private static final String SUFFIX = "instance-binding";

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

		"spring.cloud.appbroker.services[0].apps[0].name=" + BACKING_APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH
	})
	void deployAppsOnCreateService() {
		// when a service instance is created
		createServiceInstance(SI_NAME);

		// when bind an app to the service instance
		pushAppAndBind(BOUND_APP_NAME, SI_NAME);

		// then app receives binding credentials in credhub-ref
		Map<String, Object> vcapCredentials = getVcapServiceCredentials(BOUND_APP_NAME, APP_SERVICE_NAME);
		assertThat(vcapCredentials).containsOnlyKeys("credhub-ref");

		// clean up
		unbind(BOUND_APP_NAME, SI_NAME);
		deleteServiceInstance(SI_NAME);
		deleteApp(BOUND_APP_NAME);
	}
}
