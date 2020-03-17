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

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CreateInstanceWithOnlyServicesAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String SI_NAME = "si-create-only-services";

	private static final String BACKING_SI_1_NAME = "backing-service-instance-created";

	private static final String BACKING_SI_2_NAME = "backing-service-instance-existing";

	private static final String SUFFIX = "create-instance-with-only-services";

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
		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_1_NAME,
	})
	void createSingleServiceOnCreateService() {
		// given that an existing backing service is available in the marketplace
		createServiceInstance(BACKING_SERVICE_NAME, PLAN_NAME, BACKING_SI_2_NAME, emptyMap());

		// when a new service instance is created
		createServiceInstance(SI_NAME);

		// then a new backing service is created
		assertThat(getServiceInstance(BACKING_SI_1_NAME)).isNotNull();

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// then the new backing service is deleted
		assertThat(listServiceInstances()).doesNotContain(BACKING_SI_1_NAME);
		// and the existing backing service is not deleted
		assertThat(listServiceInstances()).contains(BACKING_SI_2_NAME);
	}

	@AfterEach
	void cleanUp() {
		deleteServiceInstance(BACKING_SI_2_NAME);
	}

}
