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

package org.springframework.cloud.appbroker.workflow.instance;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import org.springframework.cloud.appbroker.deployer.DeploymentProperties;
import org.springframework.cloud.appbroker.deployer.ServicesSpec;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.manager.BackingAppManagementService;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AppDeploymentDeleteServiceInstanceWorkflowTest {

	@Mock
	private BackingAppDeploymentService backingAppDeploymentService;

	@Mock
	private BackingAppManagementService backingAppManagementService;

	@Mock
	private TargetService targetService;

	@Mock
	private CredentialProviderService credentialProviderService;

	@Mock
	private BackingServicesProvisionService backingServicesProvisionService;

	private BackingApplications backingApps;

	private BackingServices backingServices;

	private BackingServices backingServices2;

	private TargetSpec targetSpec;

	private DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow;

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

		this.backingServices = BackingServices
			.builder()
			.backingService(BackingService
				.builder()
				.name("my-service")
				.plan("a-plan")
				.serviceInstanceName("my-service-instance")
				.build())
			.build();

		this.backingServices2 = BackingServices
			.builder()
			.backingService(BackingService
				.builder()
				.name("my-service2")
				.plan("a-plan2")
				.serviceInstanceName("my-service-instance2")
				.properties(Collections.singletonMap(DeploymentProperties.TARGET_PROPERTY_KEY, "my-space2"))
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
			.service(BrokeredService.builder()
				.serviceName("service2")
				.planName("plan2")
				.services(backingServices)
				.target(targetSpec)
				.build())
			.service(BrokeredService.builder()
				.serviceName("service3")
				.planName("plan3")
				.apps(backingApps)
				.services(backingServices2)
				.target(targetSpec)
				.build())
			.build();

		deleteServiceInstanceWorkflow =
			new AppDeploymentDeleteServiceInstanceWorkflow(
				brokeredServices,
				backingAppDeploymentService,
				backingAppManagementService, backingServicesProvisionService,
				credentialProviderService,
				targetService
			);
	}

	@Test
	void deleteServiceInstanceWithDeployedAppsAndBoundServicesSucceeds() {
		DeleteServiceInstanceRequest request = buildRequest("service1", "plan1");
		DeleteServiceInstanceResponse response = DeleteServiceInstanceResponse.builder().build();

		given(this.backingAppDeploymentService.undeploy(eq(backingApps)))
			.willReturn(Flux.just("undeployed1", "undeployed2"));

		// configured backing services
		given(this.targetService.addToBackingServices(eq(backingServices), eq(targetSpec), eq("service-instance-id")))
			.willReturn(Mono.just(backingServices));

		// services bound to deployed apps
		given(this.backingAppManagementService.getDeployedBackingApplications(request.getServiceInstanceId(),
			request.getServiceDefinition().getName(), request.getPlan().getName()))
			.willReturn(Mono.just(getExistingBackingAppsWithService("my-service-instance")));
		given(this.credentialProviderService.deleteCredentials(eq(backingApps), eq(request.getServiceInstanceId())))
			.willReturn(Mono.just(backingApps));
		given(this.targetService.addToBackingApplications(eq(backingApps), eq(targetSpec), eq("service-instance-id")))
			.willReturn(Mono.just(backingApps));

		given(this.backingServicesProvisionService.deleteServiceInstance(argThat(backingServices -> {
			boolean nameMatch = "my-service-instance".equals(backingServices.get(0).getServiceInstanceName());
			boolean sizeMatch = backingServices.size() == 1;
			return sizeMatch && nameMatch;
		}))).willReturn(Flux.just("my-service-instance"));

		StepVerifier
			.create(deleteServiceInstanceWorkflow.delete(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(this.backingServicesProvisionService, Mockito.times(1)).deleteServiceInstance(any());
		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void deleteServiceInstanceSucceedsWhenBackingServicesDifferFromConfiguration() {
		DeleteServiceInstanceRequest request = buildRequest("service3", "plan3");
		DeleteServiceInstanceResponse response = DeleteServiceInstanceResponse.builder().build();

		given(this.backingAppDeploymentService.undeploy(eq(backingApps)))
			.willReturn(Flux.just("undeployed1", "undeployed2"));

		// configured backing services
		given(this.targetService.addToBackingServices(eq(backingServices2), eq(targetSpec), eq("service-instance-id")))
			.willReturn(Mono.just(backingServices2));

		// different bound services
		given(this.backingAppManagementService.getDeployedBackingApplications(request.getServiceInstanceId(),
			request.getServiceDefinition().getName(), request.getPlan().getName()))
			.willReturn(Mono.just(getExistingBackingAppsWithService("different-service-instance")));
		given(this.credentialProviderService.deleteCredentials(eq(backingApps), eq(request.getServiceInstanceId())))
			.willReturn(Mono.just(backingApps));
		given(this.targetService.addToBackingApplications(eq(backingApps), eq(targetSpec), eq("service-instance-id")))
			.willReturn(Mono.just(backingApps));

		given(this.backingServicesProvisionService.deleteServiceInstance(argThat(backingServices -> {
			boolean nameMatch0 = "my-service-instance2".equals(backingServices.get(0).getServiceInstanceName());
			boolean spaceMatch0 = "my-space2".equals(backingServices.get(0).getProperties()
				.get(DeploymentProperties.TARGET_PROPERTY_KEY));
			boolean nameMatch1 = "different-service-instance".equals(backingServices.get(1).getServiceInstanceName());
			boolean spaceMatch1 = "TargetSpace".equals(backingServices.get(1).getProperties()
				.get(DeploymentProperties.TARGET_PROPERTY_KEY));
			boolean sizeMatch = backingServices.size() == 2;
			return sizeMatch && (nameMatch0 && spaceMatch0 || nameMatch1 && spaceMatch1);
		}))).willReturn(Flux.just("different-service-instance"));

		StepVerifier.create(deleteServiceInstanceWorkflow.delete(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(this.backingServicesProvisionService, Mockito.times(1)).deleteServiceInstance(any());
		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void deleteServiceInstanceWithWithNoAppsDoesNothing() {
		DeleteServiceInstanceRequest request = buildRequest("unsupported-service", "plan1");
		DeleteServiceInstanceResponse response = DeleteServiceInstanceResponse.builder().build();

		given(this.backingAppManagementService.getDeployedBackingApplications(request.getServiceInstanceId(),
			request.getServiceDefinition().getName(), request.getPlan().getName()))
			.willReturn(Mono.empty());

		StepVerifier
			.create(deleteServiceInstanceWorkflow.delete(request, response))
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void deleteServiceInstanceWithOnlyBoundServicesSucceeds() {
		DeleteServiceInstanceRequest request = buildRequest("service2", "plan2");
		DeleteServiceInstanceResponse response = DeleteServiceInstanceResponse.builder().build();

		// configured backing services
		given(this.targetService.addToBackingServices(eq(backingServices), eq(targetSpec), eq("service-instance-id")))
			.willReturn(Mono.just(backingServices));

		// no backing apps
		given(this.backingAppManagementService.getDeployedBackingApplications(request.getServiceInstanceId(),
			request.getServiceDefinition().getName(), request.getPlan().getName()))
			.willReturn(Mono.empty());

		given(this.backingServicesProvisionService.deleteServiceInstance(argThat(backingServices -> {
			boolean nameMatch = "my-service-instance".equals(backingServices.get(0).getServiceInstanceName());
			boolean sizeMatch = backingServices.size() == 1;
			return sizeMatch && nameMatch;
		}))).willReturn(Flux.just("my-service-instance"));

		StepVerifier.create(deleteServiceInstanceWorkflow.delete(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(this.backingServicesProvisionService, Mockito.times(1)).deleteServiceInstance(any());
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
			.plan(Plan.builder()
				.id(planName + "-id")
				.name(planName)
				.build())
			.build();
	}

	private BackingApplications getExistingBackingAppsWithService(String serviceInstanceName) {
		return BackingApplications
			.builder()
			.backingApplication(BackingApplication
				.builder()
				.name("app1")
				.services(ServicesSpec
					.builder()
					.serviceInstanceName(serviceInstanceName)
					.build())
				.build())
			.backingApplication(BackingApplication
				.builder()
				.name("app2")
				.services(ServicesSpec
					.builder()
					.serviceInstanceName(serviceInstanceName)
					.build())
				.build())
			.build();
	}

}
