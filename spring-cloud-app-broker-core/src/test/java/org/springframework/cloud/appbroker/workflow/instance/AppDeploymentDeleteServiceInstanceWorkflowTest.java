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
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AppDeploymentDeleteServiceInstanceWorkflowTest {

	@Mock
	private BackingAppDeploymentService backingAppDeploymentService;

	@Mock
	private TargetService targetService;

	@Mock
	private CredentialProviderService credentialProviderService;

	@Mock
	private BackingServicesProvisionService backingServicesProvisionService;

	private BackingApplications backingApps;
	private BackingServices backingServices;
	private TargetSpec targetSpec;

	private DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow;

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

		targetSpec = TargetSpec.builder().name("TargetSpace").build();
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

		deleteServiceInstanceWorkflow =
			new AppDeploymentDeleteServiceInstanceWorkflow(
				brokeredServices,
				backingAppDeploymentService,
				credentialProviderService,
				targetService,
				backingServicesProvisionService);
	}

	@Test
	void deleteServiceInstanceSucceeds() {
		DeleteServiceInstanceRequest request = buildRequest("service1", "plan1");

		given(this.backingAppDeploymentService.undeploy(eq(backingApps)))
			.willReturn(Flux.just("undeployed1", "undeployed2"));
		given(this.credentialProviderService.deleteCredentials(eq(backingApps), eq(request.getServiceInstanceId())))
			.willReturn(Mono.just(backingApps));
		given(this.targetService.addToBackingApplications(eq(backingApps), eq(targetSpec), eq("service-instance-id")))
			.willReturn(Mono.just(backingApps));
		given(this.targetService.addToBackingServices(eq(backingServices), eq(targetSpec), eq("service-instance-id")))
			.willReturn(Mono.just(backingServices));
		given(this.backingServicesProvisionService.deleteServiceInstance(eq(backingServices)))
			.willReturn(Flux.just("my-service-instance"));

		StepVerifier
			.create(deleteServiceInstanceWorkflow.delete(request))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void deleteServiceInstanceWithWithNoAppsDoesNothing() {
		StepVerifier
			.create(deleteServiceInstanceWorkflow.delete(buildRequest("unsupported-service", "plan1")))
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	private void verifyNoMoreInteractionsWithServices() {
		verifyNoMoreInteractions(this.backingServicesProvisionService);
		verifyNoMoreInteractions(this.backingAppDeploymentService);
		verifyNoMoreInteractions(this.credentialProviderService);
		verifyNoMoreInteractions(this.targetService);
	}

	private DeleteServiceInstanceRequest buildRequest(String serviceName, String planName) {
		return DeleteServiceInstanceRequest
			.builder()
			.serviceDefinitionId(serviceName + "-id")
			.serviceInstanceId("service-instance-id")
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