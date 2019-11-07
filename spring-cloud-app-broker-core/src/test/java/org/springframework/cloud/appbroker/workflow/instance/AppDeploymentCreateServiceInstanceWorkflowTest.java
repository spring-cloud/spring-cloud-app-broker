/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AppDeploymentCreateServiceInstanceWorkflowTest {

	@Mock
	private BackingAppDeploymentService appDeploymentService;

	@Mock
	private BackingServicesProvisionService servicesProvisionService;

	@Mock
	private BackingApplicationsParametersTransformationService appsParametersTransformationService;

	@Mock
	private BackingServicesParametersTransformationService servicesParametersTransformationService;

	@Mock
	private CredentialProviderService credentialProviderService;

	@Mock
	private TargetService targetService;

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
				.path("https://myfiles/app1.jar")
				.build())
			.backingApplication(BackingApplication
				.builder()
				.name("app2")
				.path("https://myfiles/app2.jar")
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

		targetSpec = TargetSpec.builder()
			.name("TargetSpace")
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
			appDeploymentService,
			servicesProvisionService,
			appsParametersTransformationService,
			servicesParametersTransformationService,
			credentialProviderService,
			targetService);
	}

	@Test
	@SuppressWarnings({"unchecked", "UnassignedFluxMonoInstance"})
	void createServiceInstanceSucceeds() {
		CreateServiceInstanceRequest request = buildRequest("service1", "plan1");
		CreateServiceInstanceResponse response = CreateServiceInstanceResponse.builder().build();

		setupMocks(request);

		StepVerifier
			.create(createServiceInstanceWorkflow.create(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(appDeploymentService).deploy(backingApps, request.getServiceInstanceId());
		verify(servicesProvisionService).createServiceInstance(backingServices);

		final String expectedServiceId = "service-instance-id";
		verify(targetService).addToBackingServices(backingServices, targetSpec, expectedServiceId);
		verify(targetService).addToBackingApplications(backingApps, targetSpec, expectedServiceId);

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	@SuppressWarnings("UnassignedFluxMonoInstance")
	void createServiceInstanceWithParametersSucceeds() {
		Map<String, Object> parameters = singletonMap("ENV_VAR_1", "value from parameters");

		CreateServiceInstanceRequest request = buildRequest("service1", "plan1", parameters);
		CreateServiceInstanceResponse response = CreateServiceInstanceResponse.builder().build();

		setupMocks(request);

		StepVerifier
			.create(createServiceInstanceWorkflow.create(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(appDeploymentService).deploy(backingApps, request.getServiceInstanceId());
		verify(servicesProvisionService).createServiceInstance(backingServices);

		verify(appsParametersTransformationService)
			.transformParameters(backingApps, parameters);

		verify(servicesParametersTransformationService)
			.transformParameters(backingServices, parameters);

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void createServiceInstanceWithNoAppsDoesNothing() {
		CreateServiceInstanceRequest request = buildRequest("unsupported-service", "plan1");
		CreateServiceInstanceResponse response = CreateServiceInstanceResponse.builder().build();

		StepVerifier
			.create(createServiceInstanceWorkflow.create(request, response))
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	private void setupMocks(CreateServiceInstanceRequest request) {
		given(this.appDeploymentService.deploy(eq(backingApps), eq(request.getServiceInstanceId())))
			.willReturn(Flux.just("app1", "app2"));
		given(this.servicesProvisionService.createServiceInstance(eq(backingServices)))
			.willReturn(Flux.just("my-service-instance"));

		given(
			this.appsParametersTransformationService.transformParameters(eq(backingApps), eq(request.getParameters())))
			.willReturn(Mono.just(backingApps));
		given(this.servicesParametersTransformationService
			.transformParameters(eq(backingServices), eq(request.getParameters())))
			.willReturn(Mono.just(backingServices));

		given(this.credentialProviderService.addCredentials(eq(backingApps), eq(request.getServiceInstanceId())))
			.willReturn(Mono.just(backingApps));

		given(this.targetService
			.addToBackingApplications(eq(backingApps), eq(targetSpec), eq(request.getServiceInstanceId())))
			.willReturn(Mono.just(backingApps));
		given(this.targetService
			.addToBackingServices(eq(backingServices), eq(targetSpec), eq(request.getServiceInstanceId())))
			.willReturn(Mono.just(backingServices));
	}

	private void verifyNoMoreInteractionsWithServices() {
		verifyNoMoreInteractions(this.appDeploymentService);
		verifyNoMoreInteractions(this.servicesProvisionService);
		verifyNoMoreInteractions(this.appsParametersTransformationService);
		verifyNoMoreInteractions(this.servicesParametersTransformationService);
		verifyNoMoreInteractions(this.credentialProviderService);
		verifyNoMoreInteractions(this.targetService);
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
			.plan(Plan.builder()
				.id(planName + "-id")
				.name(planName)
				.build())
			.parameters(parameters == null ? new HashMap<>() : parameters)
			.build();
	}

}
