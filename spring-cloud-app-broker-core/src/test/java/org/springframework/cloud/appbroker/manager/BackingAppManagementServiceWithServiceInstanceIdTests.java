/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.appbroker.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.ServicesSpec;
import org.springframework.cloud.appbroker.deployer.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.deployer.GetApplicationRequest;
import org.springframework.cloud.appbroker.deployer.deployer.GetApplicationResponse;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class BackingAppManagementServiceWithServiceInstanceIdTests {

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
			.backingApplication(BackingApplication.builder().name("testApp1").path("https://myfiles/app1.jar").build())
			.backingApplication(BackingApplication.builder().name("testApp2").path("https://myfiles/app2.jar").build())
			.build();

		BrokeredServices brokeredServices = BrokeredServices.builder()
			.service(BrokeredService.builder().serviceName("service1").planName("plan1").apps(this.backingApps).build())
			.build();

		this.backingAppManagementService = new BackingAppManagementService(this.managementClient, this.appDeployer,
				brokeredServices, this.targetService);
	}

	@Test
	void stopApplications() {
		doReturn(Mono.empty()).when(this.managementClient).stop(this.backingApps.get(0));
		doReturn(Mono.empty()).when(this.managementClient).stop(this.backingApps.get(1));

		given(this.targetService.addToBackingApplications(eq(this.backingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(this.backingApps));

		StepVerifier.create(this.backingAppManagementService.stop("foo-service-id", "service1", "plan1"))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(this.targetService).addToBackingApplications(eq(this.backingApps), any(), eq("foo-service-id"));
		verify(this.managementClient, times(2)).stop(any(BackingApplication.class));
		verifyNoMoreInteractions(this.appDeployer, this.targetService, this.managementClient);
	}

	@Test
	void stopApplicationsWithEmptyApplications() {
		BackingApplications emptyBackingApps = BackingApplications.builder().build();

		BrokeredServices brokeredServicesNoApps = BrokeredServices.builder()
			.service(BrokeredService.builder().serviceName("service1").planName("plan1").apps(emptyBackingApps).build())
			.build();

		this.backingAppManagementService = new BackingAppManagementService(this.managementClient, this.appDeployer,
				brokeredServicesNoApps, this.targetService);

		given(this.targetService.addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(emptyBackingApps));

		StepVerifier.create(this.backingAppManagementService.stop("foo-service-id", "service1", "plan1"))
			.verifyComplete();

		verify(this.targetService).addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id"));
		verifyNoInteractions(this.managementClient);
		verifyNoMoreInteractions(this.appDeployer, this.targetService, this.managementClient);
	}

	@Test
	void startApplications() {
		given(this.targetService.addToBackingApplications(eq(this.backingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(this.backingApps));

		doReturn(Mono.empty()).when(this.managementClient).start(this.backingApps.get(0));
		doReturn(Mono.empty()).when(this.managementClient).start(this.backingApps.get(1));

		StepVerifier.create(this.backingAppManagementService.start("foo-service-id", "service1", "plan1"))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(this.targetService).addToBackingApplications(eq(this.backingApps), any(), eq("foo-service-id"));
		verify(this.managementClient, times(2)).start(any(BackingApplication.class));
		verifyNoMoreInteractions(this.appDeployer, this.targetService, this.managementClient);
	}

	@Test
	void startApplicationsWithEmptyApplications() {
		BackingApplications emptyBackingApps = BackingApplications.builder().build();

		BrokeredServices brokeredServicesNoApps = BrokeredServices.builder()
			.service(BrokeredService.builder().serviceName("service1").planName("plan1").apps(emptyBackingApps).build())
			.build();

		this.backingAppManagementService = new BackingAppManagementService(this.managementClient, this.appDeployer,
				brokeredServicesNoApps, this.targetService);

		given(this.targetService.addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(emptyBackingApps));

		StepVerifier.create(this.backingAppManagementService.start("foo-service-id", "service1", "plan1"))
			.verifyComplete();

		verify(this.targetService).addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id"));
		verifyNoInteractions(this.managementClient);
		verifyNoMoreInteractions(this.appDeployer, this.targetService, this.managementClient);
	}

	@Test
	void restartApplications() {
		given(this.targetService.addToBackingApplications(eq(this.backingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(this.backingApps));

		doReturn(Mono.empty()).when(this.managementClient).restart(this.backingApps.get(0));
		doReturn(Mono.empty()).when(this.managementClient).restart(this.backingApps.get(1));

		StepVerifier.create(this.backingAppManagementService.restart("foo-service-id", "service1", "plan1"))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(this.targetService).addToBackingApplications(eq(this.backingApps), any(), eq("foo-service-id"));
		verify(this.managementClient, times(2)).restart(any(BackingApplication.class));
		verifyNoMoreInteractions(this.appDeployer, this.targetService, this.managementClient);
	}

	@Test
	void restartApplicationsWithEmptyApplications() {
		BackingApplications emptyBackingApps = BackingApplications.builder().build();

		BrokeredServices brokeredServicesNoApps = BrokeredServices.builder()
			.service(BrokeredService.builder().serviceName("service1").planName("plan1").apps(emptyBackingApps).build())
			.build();

		this.backingAppManagementService = new BackingAppManagementService(this.managementClient, this.appDeployer,
				brokeredServicesNoApps, this.targetService);

		given(this.targetService.addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(emptyBackingApps));

		StepVerifier.create(this.backingAppManagementService.restart("foo-service-id", "service1", "plan1"))
			.verifyComplete();

		verify(this.targetService).addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id"));
		verifyNoInteractions(this.managementClient);
		verifyNoMoreInteractions(this.appDeployer, this.targetService, this.managementClient);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getDeployedBackingApplications() {
		given(this.appDeployer.get(any(GetApplicationRequest.class))).willReturn(Mono
			.just(GetApplicationResponse.builder().name("testApp1").service("service1").service("service2").build()),
				Mono.just(GetApplicationResponse.builder().name("testApp2").service("service3").build()));

		given(this.targetService.addToBackingApplications(eq(this.backingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(this.backingApps));

		StepVerifier
			.create(this.backingAppManagementService.getDeployedBackingApplications("foo-service-id", "service1",
					"plan1"))
			.expectNext(BackingApplications.builder()
				.backingApplication(BackingApplication.builder()
					.name("testApp1")
					.services(ServicesSpec.builder().serviceInstanceName("service1").build(),
							ServicesSpec.builder().serviceInstanceName("service2").build())
					.build())
				.backingApplication(BackingApplication.builder()
					.name("testApp2")
					.services(ServicesSpec.builder().serviceInstanceName("service3").build())
					.build())
				.build())
			.verifyComplete();

		then(this.targetService).should().addToBackingApplications(eq(this.backingApps), any(), eq("foo-service-id"));
		then(this.appDeployer).should().get(argThat((req) -> "testApp1".equals(req.getName())));
		then(this.appDeployer).should().get(argThat((req) -> "testApp2".equals(req.getName())));

		verifyNoInteractions(this.managementClient);
		verifyNoMoreInteractions(this.appDeployer, this.targetService, this.managementClient);
	}

	@Test
	void restageApplications() {
		given(this.targetService.addToBackingApplications(eq(this.backingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(this.backingApps));

		doReturn(Mono.empty()).when(this.managementClient).restage(this.backingApps.get(0));
		doReturn(Mono.empty()).when(this.managementClient).restage(this.backingApps.get(1));

		StepVerifier.create(this.backingAppManagementService.restage("foo-service-id", "service1", "plan1"))
			.expectNext()
			.expectNext()
			.verifyComplete();

		verify(this.targetService).addToBackingApplications(eq(this.backingApps), any(), eq("foo-service-id"));
		verify(this.managementClient, times(2)).restage(any(BackingApplication.class));
		verifyNoMoreInteractions(this.appDeployer, this.targetService, this.managementClient);
	}

	@Test
	void restageApplicationsWithEmptyApplications() {
		BackingApplications emptyBackingApps = BackingApplications.builder().build();

		BrokeredServices brokeredServicesNoApps = BrokeredServices.builder()
			.service(BrokeredService.builder().serviceName("service1").planName("plan1").apps(emptyBackingApps).build())
			.build();

		this.backingAppManagementService = new BackingAppManagementService(this.managementClient, this.appDeployer,
				brokeredServicesNoApps, this.targetService);

		given(this.targetService.addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id")))
			.willReturn(Mono.just(emptyBackingApps));

		StepVerifier.create(this.backingAppManagementService.restage("foo-service-id", "service1", "plan1"))
			.verifyComplete();

		verify(this.targetService).addToBackingApplications(eq(emptyBackingApps), any(), eq("foo-service-id"));
		verifyNoInteractions(this.managementClient);
		verifyNoMoreInteractions(this.appDeployer, this.targetService, this.managementClient);
	}

}
