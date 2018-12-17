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
import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AppDeploymentUpdateServiceInstanceWorkflowTest {

	@Mock
	private BackingAppDeploymentService appDeploymentService;

	@Mock
	private BackingServicesProvisionService servicesProvisionService;

	@Mock
	private BackingApplicationsParametersTransformationService appsParametersTransformationService;

	@Mock
	private BackingServicesParametersTransformationService servicesParametersTransformationService;

	@Mock
	private TargetService targetService;

	private BackingApplications backingApps;
	private BackingServices backingServices;
	private TargetSpec targetSpec;

	private AppDeploymentUpdateServiceInstanceWorkflow updateServiceInstanceWorkflow;

	@BeforeEach
	void setUp() {
		backingApps = BackingApplications
			.builder()
			.backingApplication(BackingApplication.builder()
												  .name("app1")
												  .path("http://myfiles/app1.jar")
												  .build())
			.backingApplication(BackingApplication.builder()
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

		targetSpec = TargetSpec.builder()
			.name("TargetSpace")
			.build();

		BrokeredServices brokeredServices = BrokeredServices.builder()
			.service(BrokeredService.builder()
				.serviceName("service1")
				.planName("plan1")
				.apps(backingApps)
				.services(backingServices)
				.target(targetSpec)
				.build())
			.build();

		updateServiceInstanceWorkflow = new AppDeploymentUpdateServiceInstanceWorkflow(
			brokeredServices,
			appDeploymentService,
			servicesProvisionService,
			appsParametersTransformationService,
			servicesParametersTransformationService,
			targetService);
	}

	@Test
	@SuppressWarnings({"unchecked", "UnassignedFluxMonoInstance"})
	void updateServiceInstanceSucceeds() {
		UpdateServiceInstanceRequest request = buildRequest("service1", "plan1");
		UpdateServiceInstanceResponse response = UpdateServiceInstanceResponse.builder().build();

		setupMocks(request);

		StepVerifier
			.create(updateServiceInstanceWorkflow.update(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(appDeploymentService).deploy(backingApps);
		verify(servicesProvisionService).updateServiceInstance(backingServices);

		final String expectedServiceId = "service-instance-id";
		verify(targetService).addToBackingServices(backingServices, targetSpec, expectedServiceId);
		verify(targetService).addToBackingApplications(backingApps, targetSpec, expectedServiceId);

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void updateServiceInstanceWithParametersSucceeds() {
		UpdateServiceInstanceRequest request = buildRequest("service1", "plan1",
			singletonMap("ENV_VAR_1", "value from parameters"));
		UpdateServiceInstanceResponse response = UpdateServiceInstanceResponse.builder().build();

		setupMocks(request);

		StepVerifier
			.create(updateServiceInstanceWorkflow.update(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void updateServiceInstanceWithNoAppsDoesNothing() {
		UpdateServiceInstanceRequest request = buildRequest("unsupported-service", "plan1");
		UpdateServiceInstanceResponse response = UpdateServiceInstanceResponse.builder().build();

		StepVerifier
			.create(updateServiceInstanceWorkflow.update(request, response))
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	private void setupMocks(UpdateServiceInstanceRequest request) {
		given(this.appDeploymentService.deploy(eq(backingApps)))
			.willReturn(Flux.just("app1", "app2"));
		given(this.servicesProvisionService.updateServiceInstance(eq(backingServices)))
			.willReturn(Flux.just("my-service-instance"));

		given(this.appsParametersTransformationService.transformParameters(eq(backingApps), eq(request.getParameters())))
			.willReturn(Mono.just(backingApps));
		given(this.servicesParametersTransformationService.transformParameters(eq(backingServices), eq(request.getParameters())))
			.willReturn(Mono.just(backingServices));

		given(this.targetService.addToBackingApplications(eq(backingApps), eq(targetSpec), eq("service-instance-id")))
			.willReturn(Mono.just(backingApps));
		given(this.targetService.addToBackingServices(eq(backingServices), eq(targetSpec), eq(request.getServiceInstanceId())))
			.willReturn(Mono.just(backingServices));
	}

	private void verifyNoMoreInteractionsWithServices() {
		verifyNoMoreInteractions(this.appDeploymentService);
		verifyNoMoreInteractions(this.servicesProvisionService);
		verifyNoMoreInteractions(this.appsParametersTransformationService);
		verifyNoMoreInteractions(this.servicesParametersTransformationService);
		verifyNoMoreInteractions(this.targetService);
	}

	private UpdateServiceInstanceRequest buildRequest(String serviceName, String planName) {
		return buildRequest(serviceName, planName, null);
	}

	private UpdateServiceInstanceRequest buildRequest(String serviceName, String planName,
													  Map<String, Object> parameters) {
		return UpdateServiceInstanceRequest
			.builder()
			.serviceInstanceId("service-instance-id")
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
			.parameters(parameters == null ? new HashMap<>() : parameters)
			.build();
	}
}