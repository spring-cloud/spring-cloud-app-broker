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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderService;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AppDeploymentCreateServiceInstanceWorkflowTest {

	@Mock
	private BackingAppDeploymentService backingAppDeploymentService;

	@Mock
	private ParametersTransformationService parametersTransformationService;

	@Mock
	private CredentialProviderService credentialProviderService;

	@Mock
	private TargetService targetService;

	@Mock
	private BackingServicesProvisionService backingServicesProvisionService;

	private BackingApplications backingApps;
	private BackingServices backingServices;
	private TargetSpec targetSpec;

	private CreateServiceInstanceWorkflow createServiceInstanceWorkflow;

	@BeforeEach
	void setUp() {
		backingApps = BackingApplications
			.builder()
			.backingApplication(BackingApplication
				.builder()
				.name("app1")
				.path("http://myfiles/app1.jar")
				.build())
			.backingApplication(BackingApplication
				.builder()
				.name("app2")
				.path("http://myfiles/app2.jar")
				.build())
			.build();

		backingServices = BackingServices
			.builder()
			.backingService(BackingService
				.builder()
				.name("my-service")
				.plan("a-plan")
				.serviceInstanceName("my-service-instance")
				.build())
			.build();

		BrokeredServices brokeredServices = BrokeredServices
			.builder()
			.service(BrokeredService
				.builder()
				.serviceName("service1")
				.planName("plan1")
				.apps(backingApps)
				.services(backingServices)
				.target(targetSpec)
				.build())
			.build();

		createServiceInstanceWorkflow = new AppDeploymentCreateServiceInstanceWorkflow(
			brokeredServices,
			backingAppDeploymentService,
			parametersTransformationService,
			credentialProviderService,
			targetService,
			backingServicesProvisionService);
	}

	@Test
	@SuppressWarnings("unchecked")
	void createServiceInstanceSucceeds() {
		CreateServiceInstanceRequest request = buildRequest("service1", "plan1");

		setupMocks(request);

		StepVerifier
			.create(createServiceInstanceWorkflow.create(request))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(backingAppDeploymentService).deploy(backingApps);
		verify(backingServicesProvisionService).createServiceInstance(backingServices);

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void createServiceInstanceWithParametersSucceeds() {
		CreateServiceInstanceRequest request = buildRequest("service1", "plan1",
			singletonMap("ENV_VAR_1", "value from parameters"));

		setupMocks(request);

		StepVerifier
			.create(createServiceInstanceWorkflow.create(request))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(parametersTransformationService).transformParameters(backingApps, singletonMap("ENV_VAR_1", "value from parameters"));

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void createServiceInstanceWithTargetSucceeds() {
		CreateServiceInstanceRequest request = buildRequest("service1", "plan1",
			singletonMap("ENV_VAR_1", "value from parameters"));

		setupMocks(request);

		StepVerifier
			.create(createServiceInstanceWorkflow.create(request))
			.expectNext()
			.expectNext()
			.verifyComplete();

		final String expectedServiceId = "service-instance-id";
		verify(targetService).add(backingApps, targetSpec, expectedServiceId);

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void createServiceInstanceWithNoAppsDoesNothing() {
		CreateServiceInstanceRequest request = buildRequest("unsupported-service", "plan1");

		StepVerifier
			.create(createServiceInstanceWorkflow.create(request))
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	private void setupMocks(CreateServiceInstanceRequest request) {
		given(this.backingAppDeploymentService.deploy(eq(backingApps)))
			.willReturn(Flux.just("app1", "app2"));
		given(this.parametersTransformationService.transformParameters(eq(backingApps), eq(request.getParameters())))
			.willReturn(Mono.just(backingApps));
		given(this.credentialProviderService.addCredentials(eq(backingApps), eq(request.getServiceInstanceId())))
			.willReturn(Mono.just(backingApps));
		given(this.targetService.add(eq(backingApps), eq(targetSpec), eq(request.getServiceInstanceId())))
			.willReturn(Mono.just(backingApps));
		given(this.backingServicesProvisionService.createServiceInstance(eq(backingServices)))
			.willReturn(Flux.just("my-service-instance"));
	}

	private void verifyNoMoreInteractionsWithServices() {
		verifyNoMoreInteractions(this.backingAppDeploymentService);
		verifyNoMoreInteractions(this.parametersTransformationService);
		verifyNoMoreInteractions(this.credentialProviderService);
		verifyNoMoreInteractions(this.targetService);
		verifyNoMoreInteractions(this.backingServicesProvisionService);
	}

	private CreateServiceInstanceRequest buildRequest(String serviceName, String planName) {
		return buildRequest(serviceName, planName, null);
	}

	private CreateServiceInstanceRequest buildRequest(String serviceName, String planName,
													  Map<String, Object> parameters) {
		return CreateServiceInstanceRequest
			.builder()
			.serviceInstanceId("service-instance-id")
			.serviceDefinitionId(serviceName + "-id")
			.planId(planName + "-id")
			.serviceDefinition(ServiceDefinition
				.builder()
				.id(serviceName + "-id")
				.name(serviceName)
				.plans(Plan.builder()
						   .id(planName + "-id")
						   .name(planName)
						   .build())
				.build())
			.parameters(parameters == null ? new HashMap<>() : parameters)
			.build();
	}
}