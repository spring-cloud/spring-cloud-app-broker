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

import java.util.Collections;
import java.util.Optional;

import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UpdateInstanceWithNewServiceAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME = "app-update-with-new-services";

	private static final int FIRST_TEST = 1;

	private static final int SECOND_TEST = 2;

	private static final String SI_NAME = "si-update-with-new-services";

	private static final String OLD_BACKING_SI_NAME = "backing-service-instance-old";

	private static final String NEW_BACKING_SI_NAME = "backing-service-instance-new";

	private static final String SUFFIX = "update-with-new-services";

	private static final String APP_SERVICE_NAME = "app-service-" + SUFFIX;

	private static final String BACKING_SERVICE_NAME = "backing-service-" + SUFFIX;


	@Autowired
	private HealthListener healthListener;

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
	@Tag("first")
	@Order(FIRST_TEST)
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + OLD_BACKING_SI_NAME,

		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + OLD_BACKING_SI_NAME
	})
	void weCreateAService() {
		// when a service instance is created
		createServiceInstance(SI_NAME);

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummary(APP_NAME);
		assertThat(backingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the services are bound to it
		ServiceInstance backingServiceInstance = getServiceInstance(OLD_BACKING_SI_NAME);
		assertThat(backingServiceInstance.getApplications()).contains(APP_NAME);

		String path = backingApplication.get().getUrls().get(0);
		healthListener.start(path);
	}

	@Test
	@Order(SECOND_TEST)
	@Tag("last")
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + NEW_BACKING_SI_NAME,

		"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].services[0].plan=" + PLAN_NAME,
		"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + NEW_BACKING_SI_NAME
	})
	void weUpdateTheServiceInstanceWithANewBackingService() {
		// when the service instance is updated with a new service
		updateServiceInstance(SI_NAME, Collections.emptyMap());

		// then the backing application was updated with zero downtime
		healthListener.stop();
		assertThat(healthListener.getFailures()).isEqualTo(0);
		assertThat(healthListener.getSuccesses()).isGreaterThan(0);

		// then a backing application is re-deployed
		Optional<ApplicationSummary> updatedBackingApplication = getApplicationSummary(APP_NAME);
		assertThat(updatedBackingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and the new backing service is bound to it
		ServiceInstance newBackingServiceInstance = getServiceInstance(NEW_BACKING_SI_NAME);
		assertThat(newBackingServiceInstance.getApplications()).contains(APP_NAME);

		// and the old backing service is deleted
		assertThat(listServiceInstances()).doesNotContain(OLD_BACKING_SI_NAME);

		// then the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// and the backing service is deleted
		assertThat(listServiceInstances()).doesNotContain(NEW_BACKING_SI_NAME);
	}

	@Override
	@BeforeEach
	void setUp(TestInfo testInfo, BrokerProperties brokerProperties) {
		if (testInfo.getTags().contains("first")) {
			super.setUp(testInfo, brokerProperties);
		}
		else {
			setUpForBrokerUpdate(brokerProperties);
		}
	}

	@Override
	@AfterEach
	public void tearDown(TestInfo testInfo) {
		if (testInfo.getTags().contains("last")) {
			super.tearDown(testInfo);
		}
	}

}
