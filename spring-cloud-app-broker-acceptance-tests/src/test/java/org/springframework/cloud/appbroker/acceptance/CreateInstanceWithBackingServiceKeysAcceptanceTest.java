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

import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceKey;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.appbroker.acceptance.services.NoOpCreateServiceInstanceAppBindingWorkflow;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithBackingServiceKeysAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String SI_NAME = "si-create-service-keys";
	private static final String SK_NAME = "sk-create-service-keys";

	private static final String BACKING_SI_1_NAME = "backing-service-instance-created";

	private static final String SUFFIX = "create-instance-with-service-keys";

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
		"service-bindings-as-service-keys=true", //controls autoconfigure
		"debug=true" //Spring boot debug mode
	})
	void deployAppsAndCreateServiceKeyssOnBindService() {
		// given a brokered service instance is created
		createServiceInstance(SI_NAME);
		// and a backing service instance is created
		ServiceInstance backingServiceInstance = getServiceInstance(BACKING_SI_1_NAME);
		assertThat(backingServiceInstance).isNotNull();

		//when a service key is created with params
		createServiceKey(SK_NAME, SI_NAME);
		ServiceKey brokeredServiceKey = getServiceKey(SK_NAME, SI_NAME);

		//then a backing service key with params is created, whose name matches the brokered service binding id
		assertThat(listServiceKeys(BACKING_SI_1_NAME)).containsOnly(brokeredServiceKey.getId());
		ServiceKey serviceKey = getServiceKey(brokeredServiceKey.getId(), BACKING_SI_1_NAME);
		// and credentials from backing service key is returned in brokered service key
		assertThat(serviceKey.getCredentials()).isEqualTo(NoOpCreateServiceInstanceAppBindingWorkflow.CREDENTIALS);

		//when a service key is deleted
		deleteServiceKey(SK_NAME, SI_NAME);

		//then the backing service key is deleted
		assertThat(listServiceKeys(BACKING_SI_1_NAME)).isEmpty();

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// and the backing service instance is deleted
		assertThat(listServiceInstances()).doesNotContain(BACKING_SI_1_NAME);
	}

}
