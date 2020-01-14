/*
 * Copyright 2002-2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.GetServiceInstanceResponse;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class BackingAppManagementServiceTest {

	private BackingAppManagementService backingAppManagementService;

	private BackingApplications backingApps;

	@Mock
	private ManagementClient managementClient;

	@Mock
	private AppDeployer appDeployer;

	@Mock
	private TargetService targetService;

	@BeforeEach
	void setUp() {
		this.backingApps = BackingApplications.builder()
			.backingApplication(BackingApplication.builder()
				.name("testApp1")
				.path("https://myfiles/app1.jar")
				.build())
			.backingApplication(BackingApplication.builder()
				.name("testApp2")
				.path("https://myfiles/app2.jar")
				.build())
			.build();

		BrokeredServices brokeredServices = BrokeredServices
			.builder()
			.service(BrokeredService
				.builder()
				.serviceName("service1")
				.planName("plan1")
				.apps(backingApps)
				.build())
			.build();

		this.backingAppManagementService = new BackingAppManagementService(managementClient, appDeployer,
			brokeredServices,
			targetService);
	}

	@Test
	void stopApplications() {
		doReturn(Mono.empty()).when(managementClient).stop(backingApps.get(0));
		doReturn(Mono.empty()).when(managementClient).stop(backingApps.get(1));

		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.name("foo-service")
				.plan("plan1")
				.service("service1")
				.build()));

		given(targetService.addToBackingApplications(eq(backingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(backingApps));

		StepVerifier.create(backingAppManagementService.stop("foo-service-id"))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(appDeployer).getServiceInstance(any(GetServiceInstanceRequest.class));
		verify(targetService).addToBackingApplications(eq(backingApps), any(), eq("foo-service-id"));
		verify(managementClient, times(2)).stop(any(BackingApplication.class));
		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void stopApplicationsWithEmptyApplications() {
		BackingApplications emptyBackingApps = BackingApplications.builder().build();

		BrokeredServices brokeredServicesNoApps = BrokeredServices
			.builder()
			.service(BrokeredService
				.builder()
				.serviceName("service1")
				.planName("plan1")
				.apps(emptyBackingApps)
				.build())
			.build();

		this.backingAppManagementService = new BackingAppManagementService(managementClient, appDeployer,
			brokeredServicesNoApps,
			targetService);

		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.name("foo-service")
				.plan("plan1")
				.service("service1")
				.build()));

		given(targetService.addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(emptyBackingApps));

		StepVerifier.create(backingAppManagementService.stop("foo-service-id"))
			.verifyComplete();

		verify(appDeployer).getServiceInstance(any(GetServiceInstanceRequest.class));
		verify(targetService).addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id"));
		verifyNoInteractions(managementClient);
		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void stopApplicationsServiceNotFound() {
		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.build()));

		StepVerifier.create(backingAppManagementService.stop("unknown-service-id"))
			.verifyComplete();

		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void startApplications() {
		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.name("foo-service")
				.plan("plan1")
				.service("service1")
				.build()));

		given(targetService.addToBackingApplications(eq(backingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(backingApps));

		doReturn(Mono.empty()).when(managementClient).start(backingApps.get(0));
		doReturn(Mono.empty()).when(managementClient).start(backingApps.get(1));

		StepVerifier.create(backingAppManagementService.start("foo-service-id"))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(appDeployer).getServiceInstance(any(GetServiceInstanceRequest.class));
		verify(targetService).addToBackingApplications(eq(backingApps), any(), eq("foo-service-id"));
		verify(managementClient, times(2)).start(any(BackingApplication.class));
		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void startApplicationsServiceNotFound() {
		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.build()));

		StepVerifier.create(backingAppManagementService.start("unknown-service-id"))
			.verifyComplete();

		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void startApplicationsWithEmptyApplications() {
		BackingApplications emptyBackingApps = BackingApplications.builder().build();

		BrokeredServices brokeredServicesNoApps = BrokeredServices
			.builder()
			.service(BrokeredService
				.builder()
				.serviceName("service1")
				.planName("plan1")
				.apps(emptyBackingApps)
				.build())
			.build();

		this.backingAppManagementService = new BackingAppManagementService(managementClient, appDeployer,
			brokeredServicesNoApps,
			targetService);

		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.name("foo-service")
				.plan("plan1")
				.service("service1")
				.build()));

		given(targetService.addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(emptyBackingApps));

		StepVerifier.create(backingAppManagementService.start("foo-service-id"))
			.verifyComplete();

		verify(appDeployer).getServiceInstance(any(GetServiceInstanceRequest.class));
		verify(targetService).addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id"));
		verifyNoInteractions(managementClient);
		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void restartApplications() {
		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.name("foo-service")
				.plan("plan1")
				.service("service1")
				.build()));

		given(targetService.addToBackingApplications(eq(backingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(backingApps));

		doReturn(Mono.empty()).when(managementClient).restart(backingApps.get(0));
		doReturn(Mono.empty()).when(managementClient).restart(backingApps.get(1));

		StepVerifier.create(backingAppManagementService.restart("foo-service-id"))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(appDeployer).getServiceInstance(any(GetServiceInstanceRequest.class));
		verify(targetService).addToBackingApplications(eq(backingApps), any(), eq("foo-service-id"));
		verify(managementClient, times(2)).restart(any(BackingApplication.class));
		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void restartApplicationsServiceNotFound() {
		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.build()));

		StepVerifier.create(backingAppManagementService.restart("unknown-service-id"))
			.verifyComplete();

		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void restartApplicationsWithEmptyApplications() {
		BackingApplications emptyBackingApps = BackingApplications.builder().build();

		BrokeredServices brokeredServicesNoApps = BrokeredServices
			.builder()
			.service(BrokeredService
				.builder()
				.serviceName("service1")
				.planName("plan1")
				.apps(emptyBackingApps)
				.build())
			.build();

		this.backingAppManagementService = new BackingAppManagementService(managementClient, appDeployer,
			brokeredServicesNoApps,
			targetService);

		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.name("foo-service")
				.plan("plan1")
				.service("service1")
				.build()));

		given(targetService.addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(emptyBackingApps));

		StepVerifier.create(backingAppManagementService.restart("foo-service-id"))
			.verifyComplete();

		verify(appDeployer).getServiceInstance(any(GetServiceInstanceRequest.class));
		verify(targetService).addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id"));
		verifyNoInteractions(managementClient);
		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void restageApplications() {
		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.name("foo-service")
				.plan("plan1")
				.service("service1")
				.build()));

		given(targetService.addToBackingApplications(eq(backingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(backingApps));

		doReturn(Mono.empty()).when(managementClient).restage(backingApps.get(0));
		doReturn(Mono.empty()).when(managementClient).restage(backingApps.get(1));

		StepVerifier.create(backingAppManagementService.restage("foo-service-id"))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(appDeployer).getServiceInstance(any(GetServiceInstanceRequest.class));
		verify(targetService).addToBackingApplications(eq(backingApps), any(), eq("foo-service-id"));
		verify(managementClient, times(2)).restage(any(BackingApplication.class));
		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void restageApplicationsServiceNotFound() {
		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.build()));

		StepVerifier.create(backingAppManagementService.restage("unknown-service-id"))
			.verifyComplete();

		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

	@Test
	void restageApplicationsWithEmptyApplications() {
		BackingApplications emptyBackingApps = BackingApplications.builder().build();

		BrokeredServices brokeredServicesNoApps = BrokeredServices
			.builder()
			.service(BrokeredService
				.builder()
				.serviceName("service1")
				.planName("plan1")
				.apps(emptyBackingApps)
				.build())
			.build();

		this.backingAppManagementService = new BackingAppManagementService(managementClient, appDeployer,
			brokeredServicesNoApps,
			targetService);

		given(appDeployer.getServiceInstance(any(GetServiceInstanceRequest.class)))
			.willReturn(Mono.just(GetServiceInstanceResponse.builder()
				.name("foo-service")
				.plan("plan1")
				.service("service1")
				.build()));

		given(targetService.addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(emptyBackingApps));

		StepVerifier.create(backingAppManagementService.restage("foo-service-id"))
			.verifyComplete();

		verify(appDeployer).getServiceInstance(any(GetServiceInstanceRequest.class));
		verify(targetService).addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id"));
		verifyNoInteractions(managementClient);
		verifyNoMoreInteractions(appDeployer, targetService, managementClient);
	}

}
