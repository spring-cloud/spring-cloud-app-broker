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

package org.springframework.cloud.appbroker.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;

import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import org.springframework.cloud.appbroker.state.ServiceInstanceBindingStateRepository;
import org.springframework.cloud.appbroker.state.ServiceInstanceState;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceRouteBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceRouteBindingResponse.CreateServiceInstanceRouteBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceInstanceBindingServiceTests {

	@Mock
	private ServiceInstanceBindingStateRepository stateRepository;

	@Mock
	private LowOrderCreateServiceInstanceAppBindingWorkflow createServiceInstanceAppBindingWorkflow1;

	@Mock
	private HighOrderCreateServiceAppBindingInstanceWorkflow createServiceInstanceAppBindingWorkflow2;

	@Mock
	private LowOrderCreateServiceInstanceRouteBindingWorkflow createServiceInstanceRouteBindingWorkflow1;

	@Mock
	private HighOrderCreateServiceRouteBindingInstanceWorkflow createServiceInstanceRouteBindingWorkflow2;

	@Mock
	private LowOrderDeleteServiceInstanceBindingWorkflow deleteServiceInstanceBindingWorkflow1;

	@Mock
	private HighOrderDeleteServiceInstanceBindingWorkflow deleteServiceInstanceBindingWorkflow2;

	private WorkflowServiceInstanceBindingService workflowServiceInstanceBindingService;

	@BeforeEach
	void setUp() {
		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(this.stateRepository,
				Arrays.asList(this.createServiceInstanceAppBindingWorkflow1,
						this.createServiceInstanceAppBindingWorkflow2),
				Arrays.asList(this.createServiceInstanceRouteBindingWorkflow1,
						this.createServiceInstanceRouteBindingWorkflow2),
				Arrays.asList(this.deleteServiceInstanceBindingWorkflow1, this.deleteServiceInstanceBindingWorkflow2));
	}

	@Test
	void createServiceInstanceAppBindingWithNoWorkflows() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
						new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance binding completed",
						new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(this.stateRepository,
				null, null, null);

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder().appGuid("foo-guid").build())
			.build();

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("create service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED),
							eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceAppBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBindingWithNoWorkflows() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
						new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance binding completed",
						new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(this.stateRepository,
				null, null, null);

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder().route("foo-route").build())
			.build();

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("create service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED),
							eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceRouteBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceAppBinding() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
						new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance binding completed",
						new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder().appGuid("foo-guid").build())
			.build();

		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse
			.builder();
		CreateServiceInstanceAppBindingResponse builtResponse = CreateServiceInstanceAppBindingResponse.builder()
			.async(true)
			.credentials("foo", "bar")
			.operation("working2")
			.build();

		TestPublisher<Void> lowerOrderFlow = TestPublisher.create();
		TestPublisher<Void> higherOrderFlow = TestPublisher.create();

		given(this.createServiceInstanceAppBindingWorkflow1.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceAppBindingWorkflow1.create(eq(request), eq(builtResponse)))
			.willReturn(lowerOrderFlow.mono());
		given(this.createServiceInstanceAppBindingWorkflow1.buildResponse(eq(request),
				any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder.async(true).operation("working1")));

		given(this.createServiceInstanceAppBindingWorkflow2.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceAppBindingWorkflow2.create(eq(request), eq(builtResponse)))
			.willReturn(higherOrderFlow.mono());
		given(this.createServiceInstanceAppBindingWorkflow2.buildResponse(eq(request),
				any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder.credentials("foo", "bar").operation("working2")));

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("create service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED),
							eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				lowerOrderFlow.complete();
				lowerOrderFlow.assertWasNotRequested();

				higherOrderFlow.complete();
				lowerOrderFlow.assertWasRequested();

				InOrder createOrder = inOrder(this.createServiceInstanceAppBindingWorkflow1,
						this.createServiceInstanceAppBindingWorkflow2);
				createOrder.verify(this.createServiceInstanceAppBindingWorkflow2)
					.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(this.createServiceInstanceAppBindingWorkflow1)
					.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(this.createServiceInstanceAppBindingWorkflow2)
					.create(request, responseBuilder.build());
				createOrder.verify(this.createServiceInstanceAppBindingWorkflow1)
					.create(request, responseBuilder.build());
				createOrder.verifyNoMoreInteractions();

				CreateServiceInstanceAppBindingResponse r = (CreateServiceInstanceAppBindingResponse) response;

				assertThat(r).isNotNull();
				assertThat(r.getCredentials()).containsOnly(MapEntry.entry("foo", "bar"));
				assertThat(r.isAsync()).isTrue();
				assertThat(r.getOperation()).isEqualTo("working2");
			})
			.verifyComplete();
	}

	@Test
	void appBindingBuildResponseAppliesFlowsSequentially() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder().appGuid("foo-guid").build())
			.build();

		CreateServiceInstanceAppBindingResponseBuilder builder = CreateServiceInstanceAppBindingResponse.builder();

		TestPublisher<CreateServiceInstanceAppBindingResponseBuilder> response1 = TestPublisher.create();
		TestPublisher<CreateServiceInstanceAppBindingResponseBuilder> response2 = TestPublisher.create();

		CreateServiceInstanceAppBindingWorkflow flow1 = new CreateServiceInstanceAppBindingWorkflow() {
			@Override
			public Mono<CreateServiceInstanceAppBindingResponseBuilder> buildResponse(
					CreateServiceInstanceBindingRequest request,
					CreateServiceInstanceAppBindingResponseBuilder responseBuilder) {
				return response1.mono();
			}
		};

		CreateServiceInstanceAppBindingWorkflow flow2 = new CreateServiceInstanceAppBindingWorkflow() {
			@Override
			public Mono<CreateServiceInstanceAppBindingResponseBuilder> buildResponse(
					CreateServiceInstanceBindingRequest request,
					CreateServiceInstanceAppBindingResponseBuilder responseBuilder) {
				return response2.mono();
			}
		};

		Flux<CreateServiceInstanceAppBindingResponseBuilder> buildAppResponse = ReflectionTestUtils.invokeMethod(
				this.workflowServiceInstanceBindingService, "invokeAppBindingBuildResponse", builder, request,
				Arrays.asList(flow1, flow2));

		StepVerifier.create(buildAppResponse)
			.then(() -> response1.next(builder))
			.then(() -> response2.next(builder))
			.assertNext((r) -> response2.assertWasNotRequested())
			.then(response1::complete)
			.assertNext((r) -> response2.assertWasRequested())
			.then(response2::complete)
			.verifyComplete();
	}

	@Test
	void routeBindingBuildResponseAppliesFlowsSequentially() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder().appGuid("foo-guid").build())
			.build();

		CreateServiceInstanceRouteBindingResponseBuilder builder = CreateServiceInstanceRouteBindingResponse.builder();

		TestPublisher<CreateServiceInstanceRouteBindingResponseBuilder> response1 = TestPublisher.create();
		TestPublisher<CreateServiceInstanceRouteBindingResponseBuilder> response2 = TestPublisher.create();

		CreateServiceInstanceRouteBindingWorkflow flow1 = new CreateServiceInstanceRouteBindingWorkflow() {
			@Override
			public Mono<CreateServiceInstanceRouteBindingResponseBuilder> buildResponse(
					CreateServiceInstanceBindingRequest request,
					CreateServiceInstanceRouteBindingResponseBuilder responseBuilder) {
				return response1.mono();
			}
		};

		CreateServiceInstanceRouteBindingWorkflow flow2 = new CreateServiceInstanceRouteBindingWorkflow() {
			@Override
			public Mono<CreateServiceInstanceRouteBindingResponseBuilder> buildResponse(
					CreateServiceInstanceBindingRequest request,
					CreateServiceInstanceRouteBindingResponseBuilder responseBuilder) {
				return response2.mono();
			}
		};

		Flux<CreateServiceInstanceRouteBindingResponseBuilder> buildRouteResponse = ReflectionTestUtils.invokeMethod(
				this.workflowServiceInstanceBindingService, "invokeRouteBindingBuildResponse", builder, request,
				Arrays.asList(flow1, flow2));

		StepVerifier.create(buildRouteResponse)
			.then(() -> response1.next(builder))
			.then(() -> response2.next(builder))
			.assertNext((r) -> response2.assertWasNotRequested())
			.then(response1::complete)
			.assertNext((r) -> response2.assertWasRequested())
			.then(response2::complete)
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBinding() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
						new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance binding completed",
						new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder().route("foo-route").build())
			.build();

		CreateServiceInstanceRouteBindingResponseBuilder responseBuilder = CreateServiceInstanceRouteBindingResponse
			.builder();
		CreateServiceInstanceRouteBindingResponse builtResponse = CreateServiceInstanceRouteBindingResponse.builder()
			.async(true)
			.routeServiceUrl("foo-url")
			.operation("working2")
			.build();

		TestPublisher<Void> lowerOrderFlow = TestPublisher.create();
		TestPublisher<Void> higherOrderFlow = TestPublisher.create();

		given(this.createServiceInstanceRouteBindingWorkflow1.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceRouteBindingWorkflow1.create(eq(request), eq(builtResponse)))
			.willReturn(lowerOrderFlow.mono());
		given(this.createServiceInstanceRouteBindingWorkflow1.buildResponse(eq(request),
				any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder.async(true).operation("working1")));

		given(this.createServiceInstanceRouteBindingWorkflow2.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceRouteBindingWorkflow2.create(eq(request), eq(builtResponse)))
			.willReturn(higherOrderFlow.mono());
		given(this.createServiceInstanceRouteBindingWorkflow2.buildResponse(eq(request),
				any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder.routeServiceUrl("foo-url").operation("working2")));

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("create service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED),
							eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				lowerOrderFlow.complete();
				lowerOrderFlow.assertWasNotRequested();

				higherOrderFlow.complete();
				lowerOrderFlow.assertWasRequested();

				InOrder createOrder = inOrder(this.createServiceInstanceRouteBindingWorkflow1,
						this.createServiceInstanceRouteBindingWorkflow2);
				createOrder.verify(this.createServiceInstanceRouteBindingWorkflow2)
					.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(this.createServiceInstanceRouteBindingWorkflow1)
					.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(this.createServiceInstanceRouteBindingWorkflow2)
					.create(request, responseBuilder.build());
				createOrder.verify(this.createServiceInstanceRouteBindingWorkflow1)
					.create(request, responseBuilder.build());
				createOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceRouteBindingResponse.class);
				assertThat(((CreateServiceInstanceRouteBindingResponse) response).getRouteServiceUrl())
					.isEqualTo("foo-url");
				assertThat(response.isAsync()).isTrue();
				assertThat(response.getOperation()).isEqualTo("working2");
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceAppBindingWithAsyncError() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
						new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(
					Mono.just(new ServiceInstanceState(OperationState.FAILED, "create service instance binding failed",
							new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder().appGuid("foo-guid").build())
			.build();

		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse
			.builder();

		given(this.createServiceInstanceAppBindingWorkflow1.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceAppBindingWorkflow1.create(request, responseBuilder.build()))
			.willReturn(Mono.error(new RuntimeException("create foo error")));
		given(this.createServiceInstanceAppBindingWorkflow1.buildResponse(eq(request),
				any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(this.createServiceInstanceAppBindingWorkflow2.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceAppBindingWorkflow2.create(request, responseBuilder.build()))
			.willReturn(Mono.empty());
		given(this.createServiceInstanceAppBindingWorkflow2.buildResponse(eq(request),
				any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("create service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.FAILED), eq("create foo error"));
				repoOrder.verifyNoMoreInteractions();

				InOrder createOrder = inOrder(this.createServiceInstanceAppBindingWorkflow1,
						this.createServiceInstanceAppBindingWorkflow2);
				createOrder.verify(this.createServiceInstanceAppBindingWorkflow2)
					.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(this.createServiceInstanceAppBindingWorkflow1)
					.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(this.createServiceInstanceAppBindingWorkflow2)
					.create(request, responseBuilder.build());
				createOrder.verify(this.createServiceInstanceAppBindingWorkflow1)
					.create(request, responseBuilder.build());
				createOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceAppBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBindingWithAsyncError() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
						new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(
					Mono.just(new ServiceInstanceState(OperationState.FAILED, "create service instance binding failed",
							new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder().route("foo-route").build())
			.build();

		CreateServiceInstanceRouteBindingResponseBuilder responseBuilder = CreateServiceInstanceRouteBindingResponse
			.builder();

		given(this.createServiceInstanceRouteBindingWorkflow1.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceRouteBindingWorkflow1.create(request, responseBuilder.build()))
			.willReturn(Mono.error(new RuntimeException("create foo error")));
		given(this.createServiceInstanceRouteBindingWorkflow1.buildResponse(eq(request),
				any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(this.createServiceInstanceRouteBindingWorkflow2.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceRouteBindingWorkflow2.create(request, responseBuilder.build()))
			.willReturn(Mono.empty());
		given(this.createServiceInstanceRouteBindingWorkflow2.buildResponse(eq(request),
				any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("create service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.FAILED), eq("create foo error"));
				repoOrder.verifyNoMoreInteractions();

				InOrder createOrder = inOrder(this.createServiceInstanceRouteBindingWorkflow1,
						this.createServiceInstanceRouteBindingWorkflow2);
				createOrder.verify(this.createServiceInstanceRouteBindingWorkflow2)
					.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(this.createServiceInstanceRouteBindingWorkflow1)
					.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(this.createServiceInstanceRouteBindingWorkflow2)
					.create(request, responseBuilder.build());
				createOrder.verify(this.createServiceInstanceRouteBindingWorkflow1)
					.create(request, responseBuilder.build());
				createOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceRouteBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceAppBindingWithResponseError() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
			.bindResource(BindResource.builder().appGuid("foo-guid").build())
			.build();

		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse
			.builder();

		given(this.createServiceInstanceAppBindingWorkflow1.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceAppBindingWorkflow1.buildResponse(eq(request),
				any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.error(new ServiceBrokerException("create foo error")));

		given(this.createServiceInstanceAppBindingWorkflow2.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceAppBindingWorkflow2.buildResponse(eq(request),
				any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.expectErrorSatisfies(
					(e) -> assertThat(e).isInstanceOf(ServiceBrokerException.class).hasMessage("create foo error"))
			.verify();
	}

	@Test
	void createServiceInstanceRouteBindingWithResponseError() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
			.bindResource(BindResource.builder().route("foo-route").build())
			.build();

		CreateServiceInstanceRouteBindingResponseBuilder responseBuilder = CreateServiceInstanceRouteBindingResponse
			.builder();

		given(this.createServiceInstanceRouteBindingWorkflow1.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceRouteBindingWorkflow1.buildResponse(eq(request),
				any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.error(new ServiceBrokerException("create foo error")));

		given(this.createServiceInstanceRouteBindingWorkflow2.accept(request)).willReturn(Mono.just(true));
		given(this.createServiceInstanceRouteBindingWorkflow2.buildResponse(eq(request),
				any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.expectErrorSatisfies(
					(e) -> assertThat(e).isInstanceOf(ServiceBrokerException.class).hasMessage("create foo error"))
			.verify();
	}

	@Test
	void createServiceInstanceAppBindingWithNoAcceptsDoesNothing() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS,
					"create service instance started", new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(
					Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance completed",
							new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder().appGuid("foo-guid").build())
			.build();

		given(this.createServiceInstanceAppBindingWorkflow1.accept(request)).willReturn(Mono.just(false));

		given(this.createServiceInstanceAppBindingWorkflow2.accept(request)).willReturn(Mono.just(false));

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("create service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED),
							eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				then(this.createServiceInstanceAppBindingWorkflow1).shouldHaveNoMoreInteractions();
				then(this.createServiceInstanceAppBindingWorkflow2).shouldHaveNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceAppBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBindingWithNoAcceptsDoesNothing() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS,
					"create service instance started", new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(
					Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance completed",
							new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder().route("foo-route").build())
			.build();

		given(this.createServiceInstanceRouteBindingWorkflow1.accept(request)).willReturn(Mono.just(false));

		given(this.createServiceInstanceRouteBindingWorkflow2.accept(request)).willReturn(Mono.just(false));

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("create service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED),
							eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				then(this.createServiceInstanceRouteBindingWorkflow1).shouldHaveNoMoreInteractions();
				then(this.createServiceInstanceRouteBindingWorkflow2).shouldHaveNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceRouteBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void deleteServiceInstanceBindingsWithNoWorkflows() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS,
					"delete service instance started", new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(
					Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "delete service instance completed",
							new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(this.stateRepository,
				null, null, null);

		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.build();

		StepVerifier.create(this.workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("delete service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED),
							eq("delete service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(DeleteServiceInstanceBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void deleteServiceInstanceBinding() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance binding started",
						new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.SUCCEEDED, "delete service instance binding completed",
						new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.build();

		DeleteServiceInstanceBindingResponseBuilder responseBuilder = DeleteServiceInstanceBindingResponse.builder();
		DeleteServiceInstanceBindingResponse builtResponse = DeleteServiceInstanceBindingResponse.builder()
			.async(true)
			.operation("working2")
			.build();

		TestPublisher<Void> lowerOrderFlow = TestPublisher.create();
		TestPublisher<Void> higherOrderFlow = TestPublisher.create();

		given(this.deleteServiceInstanceBindingWorkflow1.accept(request)).willReturn(Mono.just(true));
		given(this.deleteServiceInstanceBindingWorkflow1.delete(eq(request), eq(builtResponse)))
			.willReturn(lowerOrderFlow.mono());
		given(this.deleteServiceInstanceBindingWorkflow1.buildResponse(eq(request),
				any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder.async(true).operation("working1")));

		given(this.deleteServiceInstanceBindingWorkflow2.accept(request)).willReturn(Mono.just(true));
		given(this.deleteServiceInstanceBindingWorkflow2.delete(eq(request), eq(builtResponse)))
			.willReturn(higherOrderFlow.mono());
		given(this.deleteServiceInstanceBindingWorkflow2.buildResponse(eq(request),
				any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder.operation("working2")));

		StepVerifier.create(this.workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("delete service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED),
							eq("delete service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				lowerOrderFlow.complete();
				lowerOrderFlow.assertWasNotRequested();

				higherOrderFlow.complete();
				lowerOrderFlow.assertWasRequested();

				InOrder deleteOrder = inOrder(this.deleteServiceInstanceBindingWorkflow1,
						this.deleteServiceInstanceBindingWorkflow2);
				deleteOrder.verify(this.deleteServiceInstanceBindingWorkflow2)
					.buildResponse(eq(request), any(DeleteServiceInstanceBindingResponseBuilder.class));
				deleteOrder.verify(this.deleteServiceInstanceBindingWorkflow1)
					.buildResponse(eq(request), any(DeleteServiceInstanceBindingResponseBuilder.class));
				deleteOrder.verify(this.deleteServiceInstanceBindingWorkflow2).delete(request, responseBuilder.build());
				deleteOrder.verify(this.deleteServiceInstanceBindingWorkflow1).delete(request, responseBuilder.build());
				deleteOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response.isAsync()).isTrue();
				assertThat(response.getOperation()).isEqualTo("working2");
			})
			.verifyComplete();
	}

	@Test
	void deleteServiceInstanceBindingWithAsyncError() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS,
					"delete service instance started", new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(Mono.just(new ServiceInstanceState(OperationState.FAILED, "delete service instance failed",
					new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.build();

		DeleteServiceInstanceBindingResponseBuilder responseBuilder = DeleteServiceInstanceBindingResponse.builder();

		given(this.deleteServiceInstanceBindingWorkflow1.accept(request)).willReturn(Mono.just(true));
		given(this.deleteServiceInstanceBindingWorkflow1.delete(request, responseBuilder.build()))
			.willReturn(Mono.error(new RuntimeException("delete foo binding error")));
		given(this.deleteServiceInstanceBindingWorkflow1.buildResponse(eq(request),
				any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(this.deleteServiceInstanceBindingWorkflow2.accept(request)).willReturn(Mono.just(true));
		given(this.deleteServiceInstanceBindingWorkflow2.delete(request, responseBuilder.build()))
			.willReturn(Mono.empty());
		given(this.deleteServiceInstanceBindingWorkflow2.buildResponse(eq(request),
				any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(this.workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("delete service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.FAILED),
							eq("delete foo binding error"));
				repoOrder.verifyNoMoreInteractions();

				InOrder deleteOrder = inOrder(this.deleteServiceInstanceBindingWorkflow1,
						this.deleteServiceInstanceBindingWorkflow2);
				deleteOrder.verify(this.deleteServiceInstanceBindingWorkflow2)
					.buildResponse(eq(request), any(DeleteServiceInstanceBindingResponseBuilder.class));
				deleteOrder.verify(this.deleteServiceInstanceBindingWorkflow1)
					.buildResponse(eq(request), any(DeleteServiceInstanceBindingResponseBuilder.class));
				deleteOrder.verify(this.deleteServiceInstanceBindingWorkflow2).delete(request, responseBuilder.build());
				deleteOrder.verify(this.deleteServiceInstanceBindingWorkflow1).delete(request, responseBuilder.build());
				deleteOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
			})
			.verifyComplete();
	}

	@Test
	void deleteServiceInstanceBindingWithResponseError() {
		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo")
			.bindingId("bar")
			.build();

		DeleteServiceInstanceBindingResponseBuilder responseBuilder = DeleteServiceInstanceBindingResponse.builder();

		given(this.deleteServiceInstanceBindingWorkflow1.accept(request)).willReturn(Mono.just(true));
		given(this.deleteServiceInstanceBindingWorkflow1.buildResponse(eq(request),
				any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.error(new ServiceBrokerException("delete foo binding error")));

		given(this.deleteServiceInstanceBindingWorkflow2.accept(request)).willReturn(Mono.just(true));
		given(this.deleteServiceInstanceBindingWorkflow2.buildResponse(eq(request),
				any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(this.workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.expectErrorSatisfies((e) -> assertThat(e).isInstanceOf(ServiceBrokerException.class)
				.hasMessage("delete foo binding error"))
			.verify();
	}

	@Test
	void deleteServiceInstanceBindingWithNoAcceptsDoesNothing() {
		given(this.stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance binding started",
						new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(Mono
				.just(new ServiceInstanceState(OperationState.SUCCEEDED, "delete service instance binding completed",
						new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.build();

		given(this.deleteServiceInstanceBindingWorkflow1.accept(request)).willReturn(Mono.just(false));

		given(this.deleteServiceInstanceBindingWorkflow2.accept(request)).willReturn(Mono.just(false));

		StepVerifier.create(this.workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.assertNext((response) -> {
				InOrder repoOrder = inOrder(this.stateRepository);
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS),
							eq("delete service instance binding started"));
				repoOrder.verify(this.stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED),
							eq("delete service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				then(this.deleteServiceInstanceBindingWorkflow1).shouldHaveNoMoreInteractions();
				then(this.deleteServiceInstanceBindingWorkflow2).shouldHaveNoMoreInteractions();

				assertThat(response).isNotNull();
			})
			.verifyComplete();
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	private interface HighOrderCreateServiceAppBindingInstanceWorkflow extends CreateServiceInstanceAppBindingWorkflow {

	}

	@Order
	private interface LowOrderCreateServiceInstanceAppBindingWorkflow extends CreateServiceInstanceAppBindingWorkflow {

	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	private interface HighOrderCreateServiceRouteBindingInstanceWorkflow
			extends CreateServiceInstanceRouteBindingWorkflow {

	}

	@Order
	private interface LowOrderCreateServiceInstanceRouteBindingWorkflow
			extends CreateServiceInstanceRouteBindingWorkflow {

	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	private interface HighOrderDeleteServiceInstanceBindingWorkflow extends DeleteServiceInstanceBindingWorkflow {

	}

	@Order
	private interface LowOrderDeleteServiceInstanceBindingWorkflow extends DeleteServiceInstanceBindingWorkflow {

	}

}
