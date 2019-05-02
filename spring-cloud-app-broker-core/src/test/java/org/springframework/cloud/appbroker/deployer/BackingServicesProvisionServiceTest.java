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

package org.springframework.cloud.appbroker.deployer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class BackingServicesProvisionServiceTest {

	@Mock
	private DeployerClient deployerClient;

	private BackingServicesProvisionService backingServicesProvisionService;
	private BackingServices backingServices;

	@BeforeEach
	void setUp() {
		backingServicesProvisionService = new BackingServicesProvisionService(deployerClient);
		backingServices = BackingServices.builder()
										 .backingService(BackingService.builder()
																	   .serviceInstanceName("si1")
																	   .name("service1")
																	   .plan("standard")
																	   .parameters(Collections.singletonMap("key1", "value1"))
																	   .build())
										 .backingService(BackingService.builder()
																	   .serviceInstanceName("si2")
																	   .name("service2")
																	   .plan("free")
																	   .build())
										 .build();
	}

	@Test
	@SuppressWarnings("UnassignedFluxMonoInstance")
	void createServiceInstance() {
		doReturn(Mono.just("si1"))
			.when(deployerClient).createServiceInstance(backingServices.get(0));
		doReturn(Mono.just("si2"))
			.when(deployerClient).createServiceInstance(backingServices.get(1));

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("si1");
		expectedValues.add("si2");

		StepVerifier.create(backingServicesProvisionService.createServiceInstance(backingServices))
			// deployments are run in parallel, so the order of completion is not predictable
			// ensure that both expected signals are sent in any order
			.expectNextMatches(expectedValues::remove)
			.expectNextMatches(expectedValues::remove)
			.verifyComplete();
	}

	@Test
	@SuppressWarnings("UnassignedFluxMonoInstance")
	void updateServiceInstance() {
		doReturn(Mono.just("updated1"))
			.when(deployerClient).updateServiceInstance(backingServices.get(0));
		doReturn(Mono.just("updated2"))
			.when(deployerClient).updateServiceInstance(backingServices.get(1));

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("updated1");
		expectedValues.add("updated2");

		StepVerifier.create(backingServicesProvisionService.updateServiceInstance(backingServices))
			.expectNextMatches(expectedValues::remove)
			.expectNextMatches(expectedValues::remove)
			.verifyComplete();

		verifyNoMoreInteractions(deployerClient);
	}

	@Test
	@SuppressWarnings("UnassignedFluxMonoInstance")
	void deleteServiceInstance() {
		doReturn(Mono.just("deleted1"))
			.when(deployerClient).deleteServiceInstance(backingServices.get(0));
		doReturn(Mono.just("deleted2"))
			.when(deployerClient).deleteServiceInstance(backingServices.get(1));

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("deleted1");
		expectedValues.add("deleted2");

		StepVerifier.create(backingServicesProvisionService.deleteServiceInstance(backingServices))
			// deployments are run in parallel, so the order of completion is not predictable
			// ensure that both expected signals are sent in any order
			.expectNextMatches(expectedValues::remove)
			.expectNextMatches(expectedValues::remove)
			.verifyComplete();
	}

}