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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AppDeploymentDeleteServiceInstanceWorkflowTest {

	private BackingAppDeploymentService backingAppDeploymentService;
	private BackingAppManagementService backingAppManagementService;

	private BackingServices backingServices;

	private TargetService targetService;
	private CredentialProviderService credentialProviderService;
	private BackingServicesProvisionService backingServicesProvisionService;

	private BackingApplications backingApps;

	private TargetSpec targetSpec;

	private DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow;

	@BeforeEach
	void setUp() {
		backingAppDeploymentService = Mockito.mock(BackingAppDeploymentService.class, new RuntimeExceptionAnswer());
		backingAppManagementService = Mockito.mock(BackingAppManagementService.class, new RuntimeExceptionAnswer());
		targetService = Mockito.mock(TargetService.class, new RuntimeExceptionAnswer());
		credentialProviderService = Mockito.mock(CredentialProviderService.class, new RuntimeExceptionAnswer());
		backingServicesProvisionService = Mockito.mock(BackingServicesProvisionService.class, new RuntimeExceptionAnswer());

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
			.service(BrokeredService.builder()
				.serviceName("service2_without_backing_app")
				.planName("plan1")
				.services(backingServices)
				.target(targetSpec)
				.build())
			.service(BrokeredService.builder()
				.serviceName("service3_without_backing_app_nor_service")
				.planName("plan1")
				.target(targetSpec)
				.build())
			.build();

		deleteServiceInstanceWorkflow =
			new AppDeploymentDeleteServiceInstanceWorkflow(
				brokeredServices,
				backingAppDeploymentService,
				backingServicesProvisionService,
				credentialProviderService,
				targetService
			);
	}

	@Test
	void deleteServiceInstanceSucceeds() {
		DeleteServiceInstanceRequest request = buildRequest("service1", "plan1");
		DeleteServiceInstanceResponse response = DeleteServiceInstanceResponse.builder().build();

		doReturn(Flux.just("undeployed1", "undeployed2"))
			.when(this.backingAppDeploymentService).undeploy(eq(backingApps));
		doReturn(Mono.just(backingApps))
			.when(this.credentialProviderService).deleteCredentials(eq(backingApps),
		eq(request.getServiceInstanceId()));
		doReturn(Mono.just(backingServices))
			.when(this.targetService).addToBackingServices(eq(backingServices),
			eq(targetSpec), eq("service-instance-id"));
		doReturn(Mono.just(backingApps))
			.when(this.targetService).addToBackingApplications(eq(backingApps),
			eq(targetSpec), eq("service-instance-id"));
		doReturn(Flux.just("my-service-instance"))
			.when(this.backingServicesProvisionService).deleteServiceInstance(argThat(backingServices -> {
				boolean nameMatch = "my-service-instance".equals(backingServices.get(0).getServiceInstanceName());
				boolean sizeMatch = backingServices.size() == 1;
				return sizeMatch && nameMatch;
			}));

		StepVerifier
			.create(deleteServiceInstanceWorkflow.delete(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	@Disabled("backing services with previous config would need fix such as metadata to be deleted")
	@Test
	void deleteServiceInstanceSucceedsWhenBackingServicesDifferFromConfiguration() {
		DeleteServiceInstanceRequest request = buildRequest("service1", "plan1");
		DeleteServiceInstanceResponse response = DeleteServiceInstanceResponse.builder().build();

		doReturn(Flux.just("undeployed1", "undeployed2"))
			.when(this.backingAppDeploymentService).undeploy(eq(backingApps));
		doReturn(Mono.just(getExistingBackingAppsWithService("different-service-instance")))
			.when(this.backingAppManagementService).getDeployedBackingApplications(eq(request.getServiceInstanceId()));
		doReturn(Mono.just(backingApps))
			.when(this.credentialProviderService).deleteCredentials(eq(backingApps), eq(request.getServiceInstanceId()));
		doReturn(Mono.just(backingServices))
			.when(this.targetService).addToBackingServices(eq(backingServices),
			eq(targetSpec), eq("service-instance-id"));
		doReturn(Mono.just(backingApps))
			.when(this.targetService).addToBackingApplications(eq(backingApps), eq(targetSpec), eq("different-service-instance"));
		doReturn(Flux.just("different-service-instance"))
			.when(this.backingServicesProvisionService).deleteServiceInstance(argThat(backingServices -> {
			boolean nameMatch = "different-service-instance".equals(backingServices.get(0).getServiceInstanceName());
			boolean sizeMatch = backingServices.size() == 1;
			return sizeMatch && nameMatch;
		}));

		StepVerifier
			.create(deleteServiceInstanceWorkflow.delete(request, response))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}

	@Test
	void deleteServiceInstanceWithUnknownBrokeredServiceDoesNothing() {
		DeleteServiceInstanceRequest request = buildRequest("unsupported-service", "plan1");
		DeleteServiceInstanceResponse response = DeleteServiceInstanceResponse.builder().build();

		StepVerifier
			.create(deleteServiceInstanceWorkflow.delete(request, response))
			.verifyComplete();

		verifyNoMoreInteractionsWithServices();
	}


	@Test
	void deleteServiceInstanceWithOnlyBackendServiceSucceeds() {
		DeleteServiceInstanceRequest request = buildRequest("service2_without_backing_app", "plan1");
		DeleteServiceInstanceResponse response = DeleteServiceInstanceResponse.builder().build();

		BackingApplications emptyBackingApps = BackingApplications.builder().build();

		doReturn(Flux.just())
			.when(this.backingAppDeploymentService).undeploy(eq(emptyBackingApps));
		doReturn(Mono.just(emptyBackingApps))
			.when(this.credentialProviderService).deleteCredentials(eq(emptyBackingApps),
			eq(request.getServiceInstanceId()));
		doReturn(Mono.just(backingServices))
			.when(this.targetService).addToBackingServices(eq(backingServices),
			eq(targetSpec), eq("service-instance-id"));
		doReturn(Mono.just(emptyBackingApps))
			.when(this.targetService).addToBackingApplications(eq(emptyBackingApps), eq(targetSpec), eq("service" +
			"-instance-id"));
		doReturn(Flux.just("my-service-instance"))
			.when(this.backingServicesProvisionService).deleteServiceInstance(argThat(backingServices -> {
			boolean nameMatch = "my-service-instance".equals(backingServices.get(0).getServiceInstanceName());
			boolean sizeMatch = backingServices.size() == 1;
			return sizeMatch && nameMatch;
		}));

		StepVerifier
			.create(deleteServiceInstanceWorkflow.delete(request, response))
			.expectNext()
			.expectNext()
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

	public static class RuntimeExceptionAnswer implements Answer<Object> {

		@Override
		public Object answer( InvocationOnMock invocation ) throws Throwable {
			throw new RuntimeException ( "Unexpected call to unstubbed mock method: " + invocation.toString() );
		}

	}

}

