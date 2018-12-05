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

import static org.assertj.core.api.Assertions.assertThat;
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
		doReturn(Mono.just("si1"))
			.when(deployerClient).updateServiceInstance(backingServices.get(0));

		StepVerifier.create(backingServicesProvisionService.updateServiceInstance(backingServices))
					.assertNext(value -> assertThat(value).isEqualTo("si1"))
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