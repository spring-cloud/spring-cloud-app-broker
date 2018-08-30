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
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformationService;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CreateServiceInstanceWorkflowTest {

	@Mock
	private BackingAppDeploymentService backingAppDeploymentService;

	@Mock
	private ParametersTransformationService parametersTransformationService;

	private BrokeredServices brokeredServices;
	private BackingApplications backingApps;

	@BeforeEach
	void setUp() {
		backingApps = BackingApplications.builder()
			.backingApplication(BackingApplication.builder()
				.name("helloworldapp")
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
	@SuppressWarnings("unchecked")
	void createServiceInstanceSucceeds() {
		CreateServiceInstanceRequest request = buildRequest("service1", "plan1");

		given(this.backingAppDeploymentService.deploy(eq(backingApps)))
			.willReturn(Mono.just("deployed"));
		given(this.parametersTransformationService.transformParameters(eq(backingApps), eq(request.getParameters())))
			.willReturn(Mono.just(backingApps));

		CreateServiceInstanceWorkflow createServiceInstanceWorkflow =
			new CreateServiceInstanceWorkflow(brokeredServices,
				backingAppDeploymentService,
				parametersTransformationService);

		StepVerifier
			.create(createServiceInstanceWorkflow.create(request))
			.expectNext("deployed")
			.verifyComplete();

		verifyNoMoreInteractions(this.backingAppDeploymentService);
		verifyNoMoreInteractions(this.parametersTransformationService);
	}

	@Test
	void createServiceInstanceWithParametersSucceeds() {
		CreateServiceInstanceRequest request = buildRequest("service1", "plan1",
			singletonMap("ENV_VAR_1", "value from parameters"));

		given(this.backingAppDeploymentService.deploy(eq(backingApps)))
			.willReturn(Mono.just("deployment-id-app1"));
		given(this.parametersTransformationService.transformParameters(eq(backingApps), eq(request.getParameters())))
			.willReturn(Mono.just(backingApps));

		CreateServiceInstanceWorkflow createServiceInstanceWorkflow =
			new CreateServiceInstanceWorkflow(brokeredServices,
				backingAppDeploymentService,
				parametersTransformationService);

		StepVerifier
			.create(createServiceInstanceWorkflow.create(request))
			.expectNext("deployment-id-app1")
			.verifyComplete();

		verifyNoMoreInteractions(this.backingAppDeploymentService);
		verifyNoMoreInteractions(this.parametersTransformationService);
	}

	@Test
	void createServiceInstanceFailsWithMisconfigurationFails() {
		CreateServiceInstanceWorkflow createServiceInstanceWorkflow =
			new CreateServiceInstanceWorkflow(brokeredServices,
				backingAppDeploymentService,
				parametersTransformationService);

		StepVerifier
			.create(createServiceInstanceWorkflow.create(buildRequest("unsupported-service", "plan1")))
			.expectError(ServiceBrokerException.class)
			.verify();

		verifyNoMoreInteractions(this.backingAppDeploymentService);
		verifyNoMoreInteractions(this.parametersTransformationService);
	}

	private CreateServiceInstanceRequest buildRequest(String serviceName, String planName) {
		return buildRequest(serviceName, planName, null);
	}

	private CreateServiceInstanceRequest buildRequest(String serviceName, String planName,
													  Map<String, Object> parameters) {
		return CreateServiceInstanceRequest.builder()
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
			.parameters(parameters != null ? parameters : new HashMap<>())
			.build();
	}
}