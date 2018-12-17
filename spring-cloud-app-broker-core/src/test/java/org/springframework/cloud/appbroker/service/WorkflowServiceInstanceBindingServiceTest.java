/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceInstanceBindingServiceTest {

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
		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(stateRepository,
			Arrays.asList(createServiceInstanceAppBindingWorkflow1, createServiceInstanceAppBindingWorkflow2),
			Arrays.asList(createServiceInstanceRouteBindingWorkflow1, createServiceInstanceRouteBindingWorkflow2),
			Arrays.asList(deleteServiceInstanceBindingWorkflow1, deleteServiceInstanceBindingWorkflow2));
	}

	@Test
	void createServiceInstanceAppBindingWithNoWorkflows() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance binding completed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(
				this.stateRepository, null, null, null);

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder()
				.appGuid("foo-guid")
				.build())
			.build();

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("create service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED), eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceAppBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBindingWithNoWorkflows() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance binding completed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(
				this.stateRepository, null, null, null);

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder()
				.route("foo-route")
				.build())
			.build();

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("create service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED), eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceRouteBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceAppBinding() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance binding completed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder()
				.appGuid("foo-guid")
				.build())
			.build();

		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse.builder();
		CreateServiceInstanceAppBindingResponse builtResponse = CreateServiceInstanceAppBindingResponse.builder()
			.async(true)
			.credentials("foo", "bar")
			.operation("working2")
			.build();

		TestPublisher<Void> lowerOrderFlow = TestPublisher.create();
		TestPublisher<Void> higherOrderFlow = TestPublisher.create();

		given(createServiceInstanceAppBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceAppBindingWorkflow1.create(eq(request), eq(builtResponse)))
			.willReturn(lowerOrderFlow.mono());
		given(createServiceInstanceAppBindingWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.async(true)
				.operation("working1")));

		given(createServiceInstanceAppBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceAppBindingWorkflow2.create(eq(request), eq(builtResponse)))
			.willReturn(higherOrderFlow.mono());
		given(createServiceInstanceAppBindingWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.credentials("foo", "bar")
				.operation("working2")));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("create service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED), eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				lowerOrderFlow.complete();
				lowerOrderFlow.assertWasNotRequested();

				higherOrderFlow.complete();
				lowerOrderFlow.assertWasRequested();

				InOrder createOrder = inOrder(createServiceInstanceAppBindingWorkflow1, createServiceInstanceAppBindingWorkflow2);
				createOrder.verify(createServiceInstanceAppBindingWorkflow2).buildResponse(eq(request),
					any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceAppBindingWorkflow1).buildResponse(eq(request),
					any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceAppBindingWorkflow2).create(request, responseBuilder.build());
				createOrder.verify(createServiceInstanceAppBindingWorkflow1).create(request, responseBuilder.build());
				createOrder.verifyNoMoreInteractions();

				CreateServiceInstanceAppBindingResponse r = (CreateServiceInstanceAppBindingResponse)response;

				assertThat(r).isNotNull();
				assertThat(r.getCredentials()).containsOnly(entry("foo", "bar"));
				assertThat(r.isAsync()).isTrue();
				assertThat(r.getOperation()).isEqualTo("working2");
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBinding() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance binding completed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder()
				.route("foo-route")
				.build())
			.build();

		CreateServiceInstanceRouteBindingResponseBuilder responseBuilder = CreateServiceInstanceRouteBindingResponse.builder();
		CreateServiceInstanceRouteBindingResponse builtResponse = CreateServiceInstanceRouteBindingResponse.builder()
			.async(true)
			.routeServiceUrl("foo-url")
			.operation("working2")
			.build();

		TestPublisher<Void> lowerOrderFlow = TestPublisher.create();
		TestPublisher<Void> higherOrderFlow = TestPublisher.create();

		given(createServiceInstanceRouteBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceRouteBindingWorkflow1.create(eq(request), eq(builtResponse)))
			.willReturn(lowerOrderFlow.mono());
		given(createServiceInstanceRouteBindingWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.async(true)
				.operation("working1")));

		given(createServiceInstanceRouteBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceRouteBindingWorkflow2.create(eq(request), eq(builtResponse)))
			.willReturn(higherOrderFlow.mono());
		given(createServiceInstanceRouteBindingWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.routeServiceUrl("foo-url")
				.operation("working2")));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("create service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED), eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();
				
				lowerOrderFlow.complete();
				lowerOrderFlow.assertWasNotRequested();

				higherOrderFlow.complete();
				lowerOrderFlow.assertWasRequested();

				InOrder createOrder = inOrder(createServiceInstanceRouteBindingWorkflow1, createServiceInstanceRouteBindingWorkflow2);
				createOrder.verify(createServiceInstanceRouteBindingWorkflow2).buildResponse(eq(request),
					any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceRouteBindingWorkflow1).buildResponse(eq(request),
					any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceRouteBindingWorkflow2).create(request, responseBuilder.build());
				createOrder.verify(createServiceInstanceRouteBindingWorkflow1).create(request, responseBuilder.build());
				createOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceRouteBindingResponse.class);
				assertThat(((CreateServiceInstanceRouteBindingResponse)response).getRouteServiceUrl()).isEqualTo("foo-url");
				assertThat(response.isAsync()).isTrue();
				assertThat(response.getOperation()).isEqualTo("working2");
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceAppBindingWithAsyncError() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.FAILED, "create service instance binding failed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder()
				.appGuid("foo-guid")
				.build())
			.build();

		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse.builder();

		given(createServiceInstanceAppBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceAppBindingWorkflow1.create(request, responseBuilder.build()))
			.willReturn(Mono.error(new RuntimeException("create foo error")));
		given(createServiceInstanceAppBindingWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(createServiceInstanceAppBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceAppBindingWorkflow2.create(request, responseBuilder.build()))
			.willReturn(Mono.empty());
		given(createServiceInstanceAppBindingWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("create service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.FAILED), eq("create foo error"));
				repoOrder.verifyNoMoreInteractions();

				InOrder createOrder = inOrder(createServiceInstanceAppBindingWorkflow1, createServiceInstanceAppBindingWorkflow2);
				createOrder.verify(createServiceInstanceAppBindingWorkflow2).buildResponse(eq(request),
					any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceAppBindingWorkflow1).buildResponse(eq(request),
					any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceAppBindingWorkflow2).create(request, responseBuilder.build());
				createOrder.verify(createServiceInstanceAppBindingWorkflow1).create(request, responseBuilder.build());
				createOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceAppBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBindingWithAsyncError() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.FAILED, "create service instance binding failed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder()
				.route("foo-route")
				.build())
			.build();

		CreateServiceInstanceRouteBindingResponseBuilder responseBuilder = CreateServiceInstanceRouteBindingResponse.builder();

		given(createServiceInstanceRouteBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceRouteBindingWorkflow1.create(request, responseBuilder.build()))
			.willReturn(Mono.error(new RuntimeException("create foo error")));
		given(createServiceInstanceRouteBindingWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(createServiceInstanceRouteBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceRouteBindingWorkflow2.create(request, responseBuilder.build()))
			.willReturn(Mono.empty());
		given(createServiceInstanceRouteBindingWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("create service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.FAILED), eq("create foo error"));
				repoOrder.verifyNoMoreInteractions();

				InOrder createOrder = inOrder(createServiceInstanceRouteBindingWorkflow1, createServiceInstanceRouteBindingWorkflow2);
				createOrder.verify(createServiceInstanceRouteBindingWorkflow2).buildResponse(eq(request),
					any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceRouteBindingWorkflow1).buildResponse(eq(request),
					any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceRouteBindingWorkflow2).create(request, responseBuilder.build());
				createOrder.verify(createServiceInstanceRouteBindingWorkflow1).create(request, responseBuilder.build());
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
			.bindResource(BindResource.builder()
				.appGuid("foo-guid")
				.build())
			.build();

		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse.builder();

		given(createServiceInstanceAppBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceAppBindingWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.error(new ServiceBrokerException("create foo error")));

		given(createServiceInstanceAppBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceAppBindingWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.expectErrorSatisfies(e -> assertThat(e)
				.isInstanceOf(ServiceBrokerException.class)
				.hasMessage("create foo error"))
			.verify();
	}

	@Test
	void createServiceInstanceRouteBindingWithResponseError() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
			.bindResource(BindResource.builder()
				.route("foo-route")
				.build())
			.build();

		CreateServiceInstanceRouteBindingResponseBuilder responseBuilder = CreateServiceInstanceRouteBindingResponse.builder();

		given(createServiceInstanceRouteBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceRouteBindingWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.error(new ServiceBrokerException("create foo error")));

		given(createServiceInstanceRouteBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceRouteBindingWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.expectErrorSatisfies(e -> assertThat(e)
				.isInstanceOf(ServiceBrokerException.class)
				.hasMessage("create foo error"))
			.verify();
	}

	@Test
	void createServiceInstanceAppBindingWithNoAcceptsDoesNothing() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance completed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder()
				.appGuid("foo-guid")
				.build())
			.build();

		given(createServiceInstanceAppBindingWorkflow1.accept(request))
			.willReturn(Mono.just(false));

		given(createServiceInstanceAppBindingWorkflow2.accept(request))
			.willReturn(Mono.just(false));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("create service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED), eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				verifyNoMoreInteractions(createServiceInstanceAppBindingWorkflow1, createServiceInstanceAppBindingWorkflow2);

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceAppBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBindingWithNoAcceptsDoesNothing() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance completed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder()
				.route("foo-route")
				.build())
			.build();

		given(createServiceInstanceRouteBindingWorkflow1.accept(request))
			.willReturn(Mono.just(false));

		given(createServiceInstanceRouteBindingWorkflow2.accept(request))
			.willReturn(Mono.just(false));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("create service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED), eq("create service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				verifyNoMoreInteractions(createServiceInstanceRouteBindingWorkflow1, createServiceInstanceRouteBindingWorkflow2);

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceRouteBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void deleteServiceInstanceBindingsWithNoWorkflows() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "delete service instance completed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(
				this.stateRepository, null, null, null);

		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.build();

		StepVerifier.create(workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("delete service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED), eq("delete service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(DeleteServiceInstanceBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void deleteServiceInstanceBinding() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance binding started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "delete service instance binding completed",
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

		given(deleteServiceInstanceBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(deleteServiceInstanceBindingWorkflow1.delete(eq(request), eq(builtResponse)))
			.willReturn(lowerOrderFlow.mono());
		given(deleteServiceInstanceBindingWorkflow1.buildResponse(eq(request), any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.async(true)
				.operation("working1")));

		given(deleteServiceInstanceBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(deleteServiceInstanceBindingWorkflow2.delete(eq(request), eq(builtResponse)))
			.willReturn(higherOrderFlow.mono());
		given(deleteServiceInstanceBindingWorkflow2.buildResponse(eq(request), any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.operation("working2")));

		StepVerifier.create(workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("delete service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED), eq("delete service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				lowerOrderFlow.complete();
				lowerOrderFlow.assertWasNotRequested();

				higherOrderFlow.complete();
				lowerOrderFlow.assertWasRequested();

				InOrder deleteOrder = inOrder(deleteServiceInstanceBindingWorkflow1, deleteServiceInstanceBindingWorkflow2);
				deleteOrder.verify(deleteServiceInstanceBindingWorkflow2).buildResponse(eq(request),
					any(DeleteServiceInstanceBindingResponseBuilder.class));
				deleteOrder.verify(deleteServiceInstanceBindingWorkflow1).buildResponse(eq(request),
					any(DeleteServiceInstanceBindingResponseBuilder.class));
				deleteOrder.verify(deleteServiceInstanceBindingWorkflow2).delete(request, responseBuilder.build());
				deleteOrder.verify(deleteServiceInstanceBindingWorkflow1).delete(request, responseBuilder.build());
				deleteOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response.isAsync()).isTrue();
				assertThat(response.getOperation()).isEqualTo("working2");
			})
			.verifyComplete();
	}

	@Test
	void deleteServiceInstanceBindingWithAsyncError() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.FAILED, "delete service instance failed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.build();

		DeleteServiceInstanceBindingResponseBuilder responseBuilder = DeleteServiceInstanceBindingResponse.builder();

		given(deleteServiceInstanceBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(deleteServiceInstanceBindingWorkflow1.delete(request, responseBuilder.build()))
			.willReturn(Mono.error(new RuntimeException("delete foo binding error")));
		given(deleteServiceInstanceBindingWorkflow1.buildResponse(eq(request), any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(deleteServiceInstanceBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(deleteServiceInstanceBindingWorkflow2.delete(request, responseBuilder.build()))
			.willReturn(Mono.empty());
		given(deleteServiceInstanceBindingWorkflow2.buildResponse(eq(request), any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("delete service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.FAILED), eq("delete foo binding error"));
				repoOrder.verifyNoMoreInteractions();

				InOrder deleteOrder = inOrder(deleteServiceInstanceBindingWorkflow1, deleteServiceInstanceBindingWorkflow2);
				deleteOrder.verify(deleteServiceInstanceBindingWorkflow2).buildResponse(eq(request),
					any(DeleteServiceInstanceBindingResponseBuilder.class));
				deleteOrder.verify(deleteServiceInstanceBindingWorkflow1).buildResponse(eq(request),
					any(DeleteServiceInstanceBindingResponseBuilder.class));
				deleteOrder.verify(deleteServiceInstanceBindingWorkflow2).delete(request, responseBuilder.build());
				deleteOrder.verify(deleteServiceInstanceBindingWorkflow1).delete(request, responseBuilder.build());
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

		given(deleteServiceInstanceBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(deleteServiceInstanceBindingWorkflow1.buildResponse(eq(request), any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.error(new ServiceBrokerException("delete foo binding error")));

		given(deleteServiceInstanceBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(deleteServiceInstanceBindingWorkflow2.buildResponse(eq(request), any(DeleteServiceInstanceBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.expectErrorSatisfies(e -> assertThat(e)
				.isInstanceOf(ServiceBrokerException.class)
				.hasMessage("delete foo binding error"))
			.verify();
	}

	@Test
	void deleteServiceInstanceBindingWithNoAcceptsDoesNothing() {
		when(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance binding started",
				new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "delete service instance binding completed",
				new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.build();

		given(deleteServiceInstanceBindingWorkflow1.accept(request))
			.willReturn(Mono.just(false));

		given(deleteServiceInstanceBindingWorkflow2.accept(request))
			.willReturn(Mono.just(false));

		StepVerifier.create(workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder repoOrder = inOrder(stateRepository);
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.IN_PROGRESS), eq("delete service instance binding started"));
				repoOrder.verify(stateRepository)
					.saveState(eq("foo-service"), eq("foo-binding"), eq(OperationState.SUCCEEDED), eq("delete service instance binding completed"));
				repoOrder.verifyNoMoreInteractions();

				verifyNoMoreInteractions(deleteServiceInstanceBindingWorkflow1, deleteServiceInstanceBindingWorkflow2);

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
	private interface HighOrderCreateServiceRouteBindingInstanceWorkflow extends CreateServiceInstanceRouteBindingWorkflow {
	}

	@Order
	private interface LowOrderCreateServiceInstanceRouteBindingWorkflow extends CreateServiceInstanceRouteBindingWorkflow {
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	private interface HighOrderDeleteServiceInstanceBindingWorkflow extends DeleteServiceInstanceBindingWorkflow {
	}

	@Order
	private interface LowOrderDeleteServiceInstanceBindingWorkflow extends DeleteServiceInstanceBindingWorkflow {
	}

}