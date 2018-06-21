/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.component;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is a black box test to validate the end-to-end flow of deploying an application from a service broker request,
 * starting with the entry point into this library.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
	classes = TestAppBrokerApplication.class,
	properties = "spring.cloud.app.broker.create.instance.appName=helloworldapp")
@Disabled
public class ProvisionInstanceComponentAlternativeTest {

	@Autowired
	private ServiceInstanceService serviceInstanceService;

	@Test
	void shouldPushAppWhenCreateServiceEndpointCalled() {
		CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
			.serviceDefinitionId("bdb1be2e-360b-495c-8115-d7697f9c6a9e")
			.planId("b973fb78-82f3-49ef-9b8b-c1876974a6cd")
			.serviceInstanceId("1111-1111-1111-1111")
			.build();

		CreateServiceInstanceResponse response = serviceInstanceService.createServiceInstance(request);

		assertThat(response.isInstanceExisted()).isFalse();
		assertThat(response.isAsync()).isFalse();

		// deployer app object
		// TODO assert cloudfoundry API contract for pushing our helloworld application was satisfied
		// assertThat("The CF API endpoint was called with operation").isEqualTo("push");// which is the value from the deployer
	}
}
