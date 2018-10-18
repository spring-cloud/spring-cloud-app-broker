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

import java.util.Arrays;

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

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceRouteBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceRouteBindingResponse.CreateServiceInstanceRouteBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceInstanceBindingServiceTest {

	@Mock
	private LowOrderCreateServiceInstanceAppBindingWorkflow createServiceInstanceAppBindingWorkflow1;

	@Mock
	private HighOrderCreateServiceAppBindingInstanceWorkflow createServiceInstanceAppBindingWorkflow2;

	@Mock
	private LowOrderCreateServiceInstanceRouteBindingWorkflow createServiceInstanceRouteBindingWorkflow1;

	@Mock
	private HighOrderCreateServiceRouteBindingInstanceWorkflow createServiceInstanceRouteBindingWorkflow2;

	private WorkflowServiceInstanceBindingService workflowServiceInstanceBindingService;

	@BeforeEach
	void setUp() {
		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(
			Arrays.asList(createServiceInstanceAppBindingWorkflow1, createServiceInstanceAppBindingWorkflow2),
			Arrays.asList(createServiceInstanceRouteBindingWorkflow1, createServiceInstanceRouteBindingWorkflow2));
	}

	@Test
	void createServiceInstanceAppBindingWithNoWorkflows() {
		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(null, null);

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
			.bindResource(BindResource.builder()
				.appGuid("foo-guid")
				.build())
			.build();

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceAppBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBindingWithNoWorkflows() {
		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(null, null);

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
			.bindResource(BindResource.builder()
				.route("foo-route")
				.build())
			.build();

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceRouteBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceAppBinding() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
			.bindResource(BindResource.builder()
				.appGuid("foo-guid")
				.build())
			.build();

		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse.builder();

		TestPublisher<Void> lowerOrderFlow = TestPublisher.create();
		TestPublisher<Void> higherOrderFlow = TestPublisher.create();

		given(createServiceInstanceAppBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceAppBindingWorkflow1.create(request))
			.willReturn(lowerOrderFlow.flux());
		given(createServiceInstanceAppBindingWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.async(true)
				.operation("working1")));

		given(createServiceInstanceAppBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceAppBindingWorkflow2.create(request))
			.willReturn(higherOrderFlow.flux());
		given(createServiceInstanceAppBindingWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.credentials("foo", "bar")
				.operation("working2")));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				lowerOrderFlow.complete();
				lowerOrderFlow.assertWasNotRequested();

				higherOrderFlow.complete();
				lowerOrderFlow.assertWasRequested();

				InOrder createOrder = inOrder(createServiceInstanceAppBindingWorkflow1, createServiceInstanceAppBindingWorkflow2);
				createOrder.verify(createServiceInstanceAppBindingWorkflow2).buildResponse(eq(request),
					any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceAppBindingWorkflow1).buildResponse(eq(request),
					any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceAppBindingWorkflow2).create(request);
				createOrder.verify(createServiceInstanceAppBindingWorkflow1).create(request);
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
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
			.bindResource(BindResource.builder()
				.route("foo-route")
				.build())
			.build();

		CreateServiceInstanceRouteBindingResponseBuilder responseBuilder = CreateServiceInstanceRouteBindingResponse.builder();

		TestPublisher<Void> lowerOrderFlow = TestPublisher.create();
		TestPublisher<Void> higherOrderFlow = TestPublisher.create();

		given(createServiceInstanceRouteBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceRouteBindingWorkflow1.create(request))
			.willReturn(lowerOrderFlow.flux());
		given(createServiceInstanceRouteBindingWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.async(true)
				.operation("working1")));

		given(createServiceInstanceRouteBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceRouteBindingWorkflow2.create(request))
			.willReturn(higherOrderFlow.flux());
		given(createServiceInstanceRouteBindingWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.routeServiceUrl("foo-url")
				.operation("working2")));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				lowerOrderFlow.complete();
				lowerOrderFlow.assertWasNotRequested();

				higherOrderFlow.complete();
				lowerOrderFlow.assertWasRequested();

				InOrder createOrder = inOrder(createServiceInstanceRouteBindingWorkflow1, createServiceInstanceRouteBindingWorkflow2);
				createOrder.verify(createServiceInstanceRouteBindingWorkflow2).buildResponse(eq(request),
					any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceRouteBindingWorkflow1).buildResponse(eq(request),
					any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceRouteBindingWorkflow2).create(request);
				createOrder.verify(createServiceInstanceRouteBindingWorkflow1).create(request);
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
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
			.bindResource(BindResource.builder()
				.appGuid("foo-guid")
				.build())
			.build();

		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse.builder()
			.operation(OperationState.IN_PROGRESS.toString());

		given(createServiceInstanceAppBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceAppBindingWorkflow1.create(request))
			.willReturn(Flux.error(new RuntimeException("create foo error")));
		given(createServiceInstanceAppBindingWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(createServiceInstanceAppBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceAppBindingWorkflow2.create(request))
			.willReturn(Flux.empty());
		given(createServiceInstanceAppBindingWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceAppBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder createOrder = inOrder(createServiceInstanceAppBindingWorkflow1, createServiceInstanceAppBindingWorkflow2);
				createOrder.verify(createServiceInstanceAppBindingWorkflow2).buildResponse(eq(request),
					any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceAppBindingWorkflow1).buildResponse(eq(request),
					any(CreateServiceInstanceAppBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceAppBindingWorkflow2).create(request);
				createOrder.verify(createServiceInstanceAppBindingWorkflow1).create(request);
				createOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceAppBindingResponse.class);
				assertThat(response.getOperation()).isEqualTo(OperationState.IN_PROGRESS.toString());
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBindingWithAsyncError() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
			.bindResource(BindResource.builder()
				.route("foo-route")
				.build())
			.build();

		CreateServiceInstanceRouteBindingResponseBuilder responseBuilder = CreateServiceInstanceRouteBindingResponse.builder()
			.operation(OperationState.IN_PROGRESS.toString());

		given(createServiceInstanceRouteBindingWorkflow1.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceRouteBindingWorkflow1.create(request))
			.willReturn(Flux.error(new RuntimeException("create foo error")));
		given(createServiceInstanceRouteBindingWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(createServiceInstanceRouteBindingWorkflow2.accept(request))
			.willReturn(Mono.just(true));
		given(createServiceInstanceRouteBindingWorkflow2.create(request))
			.willReturn(Flux.empty());
		given(createServiceInstanceRouteBindingWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceRouteBindingResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.assertNext(response -> {
				InOrder createOrder = inOrder(createServiceInstanceRouteBindingWorkflow1, createServiceInstanceRouteBindingWorkflow2);
				createOrder.verify(createServiceInstanceRouteBindingWorkflow2).buildResponse(eq(request),
					any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceRouteBindingWorkflow1).buildResponse(eq(request),
					any(CreateServiceInstanceRouteBindingResponseBuilder.class));
				createOrder.verify(createServiceInstanceRouteBindingWorkflow2).create(request);
				createOrder.verify(createServiceInstanceRouteBindingWorkflow1).create(request);
				createOrder.verifyNoMoreInteractions();

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceRouteBindingResponse.class);
				assertThat(response.getOperation()).isEqualTo(OperationState.IN_PROGRESS.toString());
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
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
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
				verifyNoMoreInteractions(createServiceInstanceAppBindingWorkflow1, createServiceInstanceAppBindingWorkflow2);

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceAppBindingResponse.class);
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceRouteBindingWithNoAcceptsDoesNothing() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-id")
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
				verifyNoMoreInteractions(createServiceInstanceRouteBindingWorkflow1, createServiceInstanceRouteBindingWorkflow2);

				assertThat(response).isNotNull();
				assertThat(response).isInstanceOf(CreateServiceInstanceRouteBindingResponse.class);
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

}