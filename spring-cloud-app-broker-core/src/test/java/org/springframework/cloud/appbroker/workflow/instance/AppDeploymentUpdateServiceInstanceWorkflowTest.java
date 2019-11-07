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
import reactor.core.publisher.Hooks;
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
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AppDeploymentUpdateServiceInstanceWorkflowTest {

	private static final String SERVICE_INSTANCE_ID = "service-instance-id";

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

	private BackingServices backingServicesPlan2;

	private TargetSpec targetSpec;

	private AppDeploymentUpdateServiceInstanceWorkflow updateServiceInstanceWorkflow;

	@BeforeEach
	void setUp() {
		Hooks.onOperatorDebug();
		backingApps = BackingApplications
			.builder()
			.backingApplication(BackingApplication.builder()
				.name("app1")
				.path("https://myfiles/app1.jar")
				.build())
			.backingApplication(BackingApplication.builder()
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
		backingServicesPlan2 = BackingServices
			.builder()
			.backingService(BackingService
				.builder()
				.name("my-service")
				.plan("b-plan")
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
			.service(BrokeredService.builder()
				.serviceName("service1")
				.planName("plan2")
				.apps(backingApps)
				.services(backingServicesPlan2)
				.target(targetSpec)
				.build())
			.build();

		updateServiceInstanceWorkflow = new AppDeploymentUpdateServiceInstanceWorkflow(
			brokeredServices,
			appDeploymentService,
			servicesProvisionService,
			appsParametersTransformationService,
			servicesParametersTransformationService,
			targetService, new BackingServicesUpdateValidatorService());
	}

	@Test
	@SuppressWarnings({"unchecked", "UnassignedFluxMonoInstance"})
	void updateServiceInstanceSucceeds() {
		UpdateServiceInstanceRequest request = buildRequest("service1", "plan1", null);
		UpdateServiceInstanceResponse response = UpdateServiceInstanceResponse.builder().build();

		setupMocks(request, backingServices, backingServices);

		StepVerifier
			.create(updateServiceInstanceWorkflow.update(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		then(appDeploymentService).should().update(backingApps, request.getServiceInstanceId());
		then(servicesProvisionService).should().updateServiceInstance(backingServices);

		final String expectedServiceId = SERVICE_INSTANCE_ID;
		then(targetService).should().addToBackingServices(backingServices, targetSpec, expectedServiceId);
		then(targetService).should().addToBackingApplications(backingApps, targetSpec, expectedServiceId);

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void updateServiceInstanceWithParametersSucceeds() {
		UpdateServiceInstanceRequest request = buildRequest("service1", "plan1",
			singletonMap("ENV_VAR_1", "value from parameters"), null);
		UpdateServiceInstanceResponse response = UpdateServiceInstanceResponse.builder().build();

		setupMocks(request, backingServices, backingServices);

		StepVerifier
			.create(updateServiceInstanceWorkflow.update(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	@SuppressWarnings("UnassignedFluxMonoInstance")
	@Test
	void updateServiceInstanceWithCompatiblePlanSavesPreviousPlan() {
		UpdateServiceInstanceRequest request = buildRequest("service1", "plan2", "plan1");
		UpdateServiceInstanceResponse response = UpdateServiceInstanceResponse.builder().build();

		BackingServices expectedUpdatedBackingServices = BackingServices
			.builder()
			.backingService(BackingService
				.builder()
				.name("my-service")
				.plan("b-plan")
				.previousPlan("a-plan")
				.serviceInstanceName("my-service-instance")
				.build())
			.build();


		setupMocks(request, backingServicesPlan2, expectedUpdatedBackingServices);

		StepVerifier
			.create(updateServiceInstanceWorkflow.update(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		then(appDeploymentService).should().update(backingApps, request.getServiceInstanceId());
		then(servicesProvisionService).should().updateServiceInstance(expectedUpdatedBackingServices);

		final String expectedServiceId = SERVICE_INSTANCE_ID;
		then(targetService).should().addToBackingServices(backingServicesPlan2, targetSpec, expectedServiceId);
		then(targetService).should().addToBackingApplications(backingApps, targetSpec, expectedServiceId);

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void updateServiceInstanceWithNoAppsDoesNothing() {
		UpdateServiceInstanceRequest request = buildRequest("unsupported-service", "plan1", null);
		UpdateServiceInstanceResponse response = UpdateServiceInstanceResponse.builder().build();

		StepVerifier
			.create(updateServiceInstanceWorkflow.update(request, response))
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	private void setupMocks(UpdateServiceInstanceRequest request, BackingServices backingServices,
		BackingServices updatedBackingServices) {
		given(this.appDeploymentService.update(eq(backingApps), eq(request.getServiceInstanceId())))
			.willReturn(Flux.just("app1", "app2"));
		given(this.servicesProvisionService.updateServiceInstance(eq(updatedBackingServices)))
			.willReturn(Flux.just("my-service-instance"));

		given(
			this.appsParametersTransformationService.transformParameters(eq(backingApps), eq(request.getParameters())))
			.willReturn(Mono.just(backingApps));
		boolean testingPlanUpdateMakingMultipleCallsWithDifferentArguments = !backingServices
			.equals(updatedBackingServices);
		if (testingPlanUpdateMakingMultipleCallsWithDifferentArguments) {
			given(this.servicesParametersTransformationService
				.transformParameters(any(BackingServices.class), eq(request.getParameters())))
				.willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
			given(this.targetService
				.addToBackingServices(any(BackingServices.class), eq(targetSpec), eq(request.getServiceInstanceId())))
				.willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
		}
		else {
			given(this.servicesParametersTransformationService
				.transformParameters(eq(backingServices), eq(request.getParameters())))
				.willReturn(Mono.just(backingServices));
			given(this.targetService
				.addToBackingServices(eq(backingServices), eq(targetSpec), eq(request.getServiceInstanceId())))
				.willReturn(Mono.just(backingServices));
		}
		given(this.targetService
			.addToBackingApplications(eq(backingApps), eq(targetSpec), eq(SERVICE_INSTANCE_ID)))
			.willReturn(Mono.just(backingApps));
	}

	private void verifyNoMoreInteractionsWithServices() {
		verifyNoMoreInteractions(this.appDeploymentService);
		verifyNoMoreInteractions(this.servicesProvisionService);
		verifyNoMoreInteractions(this.appsParametersTransformationService);
		verifyNoMoreInteractions(this.servicesParametersTransformationService);
		verifyNoMoreInteractions(this.targetService);
	}

	private UpdateServiceInstanceRequest buildRequest(String serviceName, String planName, String previousPlan) {
		return buildRequest(serviceName, planName, null, previousPlan);
	}

	private UpdateServiceInstanceRequest buildRequest(String serviceName, String planName,
		Map<String, Object> parameters, String previousPlan) {

		UpdateServiceInstanceRequest.UpdateServiceInstanceRequestBuilder builder = UpdateServiceInstanceRequest
			.builder()
			.serviceInstanceId(SERVICE_INSTANCE_ID)
			.serviceDefinitionId(serviceName + "-id")
			.planId(planName + "-id")
			.serviceDefinition(ServiceDefinition.builder()
				.id(serviceName + "-id")
				.name(serviceName)
				.plans(Plan.builder()
						.id("plan1-id")
						.name("plan1")
						.build(),
					Plan.builder()
						.id("plan2-id")
						.name("plan2")
						.build())
				.build())
			.plan(Plan.builder()
				.id(planName + "-id")
				.name(planName)
				.build())
			.parameters(parameters == null ? new HashMap<>() : parameters);
		if (previousPlan != null) {
			builder.previousValues(new UpdateServiceInstanceRequest.PreviousValues(previousPlan + "-id"));
		}
		return builder.build();
	}

}
