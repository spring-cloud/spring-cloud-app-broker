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

package org.springframework.cloud.appbroker.workflow.instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeleteServiceInstanceWorkflowTest {

	@Mock
	private BackingAppDeploymentService backingAppDeploymentService;

	private BrokeredServices brokeredServices;

	@BeforeEach
	void setUp() {
		BackingApplications backingApps = BackingApplications.builder()
			.backingApplication(BackingApplication.builder()
				.name("app1")
				.path("http://myfiles/app.jar")
				.build())
			.build();

		brokeredServices = BrokeredServices.builder()
			.service(BrokeredService.builder()
				.serviceName("service1")
				.planName("plan1")
				.apps(backingApps)
				.build())
			.build();
	}

	@Test
	void deleteServiceInstanceSucceeds() {
		given(this.backingAppDeploymentService.undeploy(any(BackingApplications.class)))
			.willReturn(Mono.just("undeployed"));

		DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow =
			new DeleteServiceInstanceWorkflow(brokeredServices, backingAppDeploymentService);

		// when
		StepVerifier
			.create(deleteServiceInstanceWorkflow.delete(buildRequest("service1", "plan1")))
			.expectNext("undeployed")
			.verifyComplete();

		verifyNoMoreInteractions(this.backingAppDeploymentService);
	}

	@Test
	void deleteServiceInstanceFailsWithMisconfigurationFails() {
		DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow =
			new DeleteServiceInstanceWorkflow(brokeredServices, backingAppDeploymentService);

		// when
		StepVerifier
			.create(deleteServiceInstanceWorkflow.delete(buildRequest("unsupported-service", "plan1")))
			.expectError(ServiceBrokerException.class)
			.verify();

		verifyNoMoreInteractions(this.backingAppDeploymentService);
	}

	private DeleteServiceInstanceRequest buildRequest(String serviceName, String planName) {
		return DeleteServiceInstanceRequest.builder()
			.serviceDefinitionId(serviceName + "-id")
			.planId(planName + "-id")
			.serviceDefinition(ServiceDefinition.builder()
				.id(serviceName + "-id")
				.name(serviceName)
				.plans(Plan.builder()
					.id(planName + "-id")
					.name(planName)
					.build())
				.build())
			.build();
	}
}