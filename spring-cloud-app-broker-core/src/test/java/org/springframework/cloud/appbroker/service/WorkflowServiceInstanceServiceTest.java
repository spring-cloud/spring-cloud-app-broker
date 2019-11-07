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

import org.springframework.cloud.appbroker.state.ServiceInstanceState;
import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse.DeleteServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse.UpdateServiceInstanceResponseBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceInstanceServiceTest {

	@Mock
	private ServiceInstanceStateRepository serviceInstanceStateRepository;

	@Mock
	private LowOrderCreateServiceInstanceWorkflow createServiceInstanceWorkflow1;

	@Mock
	private HighOrderCreateServiceInstanceWorkflow createServiceInstanceWorkflow2;

	@Mock
	private LowOrderDeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow1;

	@Mock
	private HighOrderDeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow2;

	@Mock
	private LowOrderUpdateServiceInstanceWorkflow updateServiceInstanceWorkflow1;

	@Mock
	private HighOrderUpdateServiceInstanceWorkflow updateServiceInstanceWorkflow2;

	private WorkflowServiceInstanceService workflowServiceInstanceService;

	@BeforeEach
	void setUp() {
		this.workflowServiceInstanceService = new WorkflowServiceInstanceService(serviceInstanceStateRepository,
				Arrays.asList(createServiceInstanceWorkflow1, createServiceInstanceWorkflow2),
				Arrays.asList(deleteServiceInstanceWorkflow1, deleteServiceInstanceWorkflow2),
				Arrays.asList(updateServiceInstanceWorkflow1, updateServiceInstanceWorkflow2));
	}

	@Test
	void createServiceInstance() {
		given(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance started",
										new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance completed",
										new Timestamp(Instant.now().minusSeconds(300).toEpochMilli()))));

		CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();
		CreateServiceInstanceResponse builtResponse = CreateServiceInstanceResponse.builder()
				.async(true)
				.dashboardUrl("https://dashboard.example.com")
				.operation("working2")
				.build();

		TestPublisher<Void> lowerOrderFlow = TestPublisher.create();
		TestPublisher<Void> higherOrderFlow = TestPublisher.create();

		given(createServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(true));
		given(createServiceInstanceWorkflow1.create(eq(request), eq(builtResponse)))
				.willReturn(lowerOrderFlow.mono());
		given(
				createServiceInstanceWorkflow1
						.buildResponse(eq(request), any(CreateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder
						.async(true)
						.operation("working1")));

		given(createServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(true));
		given(createServiceInstanceWorkflow2.create(eq(request), eq(builtResponse)))
				.willReturn(higherOrderFlow.mono());
		given(createServiceInstanceWorkflow2
				.buildResponse(eq(request), any(CreateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder
						.dashboardUrl("https://dashboard.example.com")
						.operation("working2")));

		StepVerifier.create(workflowServiceInstanceService.createServiceInstance(request))
				.assertNext(response -> {
					InOrder repoOrder = inOrder(serviceInstanceStateRepository);
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.IN_PROGRESS),
									eq("create service instance started"));
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.SUCCEEDED),
									eq("create service instance completed"));
					repoOrder.verifyNoMoreInteractions();

					lowerOrderFlow.complete();
					lowerOrderFlow.assertWasNotRequested();

					higherOrderFlow.complete();
					lowerOrderFlow.assertWasRequested();

					InOrder createOrder = inOrder(createServiceInstanceWorkflow1, createServiceInstanceWorkflow2);
					createOrder.verify(createServiceInstanceWorkflow2).buildResponse(eq(request),
							any(CreateServiceInstanceResponseBuilder.class));
					createOrder.verify(createServiceInstanceWorkflow1).buildResponse(eq(request),
							any(CreateServiceInstanceResponseBuilder.class));
					createOrder.verify(createServiceInstanceWorkflow2).create(request, responseBuilder.build());
					createOrder.verify(createServiceInstanceWorkflow1).create(request, responseBuilder.build());
					createOrder.verifyNoMoreInteractions();

					assertThat(response).isNotNull();
					assertThat(response.isAsync()).isTrue();
					assertThat(response.getDashboardUrl()).isEqualTo("https://dashboard.example.com");
					assertThat(response.getOperation()).isEqualTo("working2");
				})
				.verifyComplete();
	}

	@Test
	void createServiceInstanceWithAsyncError() {
		given(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance started",
										new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
				.willReturn(Mono.just(new ServiceInstanceState(OperationState.FAILED, "create service instance failed",
						new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();

		given(createServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(true));
		given(createServiceInstanceWorkflow1.create(request, responseBuilder.build()))
				.willReturn(Mono.error(new RuntimeException("create foo error")));
		given(
				createServiceInstanceWorkflow1
						.buildResponse(eq(request), any(CreateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder));

		given(createServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(true));
		given(createServiceInstanceWorkflow2.create(request, responseBuilder.build()))
				.willReturn(Mono.empty());
		given(
				createServiceInstanceWorkflow2
						.buildResponse(eq(request), any(CreateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceService.createServiceInstance(request))
				.assertNext(response -> {
					InOrder repoOrder = inOrder(serviceInstanceStateRepository);
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.IN_PROGRESS),
									eq("create service instance started"));
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.FAILED), eq("create foo error"));
					repoOrder.verifyNoMoreInteractions();

					InOrder createOrder = inOrder(createServiceInstanceWorkflow1, createServiceInstanceWorkflow2);
					createOrder.verify(createServiceInstanceWorkflow2).buildResponse(eq(request),
							any(CreateServiceInstanceResponseBuilder.class));
					createOrder.verify(createServiceInstanceWorkflow1).buildResponse(eq(request),
							any(CreateServiceInstanceResponseBuilder.class));
					createOrder.verify(createServiceInstanceWorkflow2).create(request, responseBuilder.build());
					createOrder.verify(createServiceInstanceWorkflow1).create(request, responseBuilder.build());
					createOrder.verifyNoMoreInteractions();

					assertThat(response).isNotNull();
				})
				.verifyComplete();
	}

	@Test
	void createServiceInstanceWithResponseError() {
		CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();

		given(createServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(true));
		given(
				createServiceInstanceWorkflow1
						.buildResponse(eq(request), any(CreateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.error(new ServiceBrokerException("create foo error")));

		given(createServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(true));
		given(
				createServiceInstanceWorkflow2
						.buildResponse(eq(request), any(CreateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceService.createServiceInstance(request))
				.expectErrorSatisfies(e -> assertThat(e)
						.isInstanceOf(ServiceBrokerException.class)
						.hasMessage("create foo error"))
				.verify();
	}

	@Test
	void createServiceInstanceWithNoAcceptsDoesNothing() {
		given(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance started",
										new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance completed",
										new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		given(createServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(false));

		given(createServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(false));

		StepVerifier.create(workflowServiceInstanceService.createServiceInstance(request))
				.assertNext(response -> {
					InOrder repoOrder = inOrder(serviceInstanceStateRepository);
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.IN_PROGRESS),
									eq("create service instance started"));
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.SUCCEEDED),
									eq("create service instance completed"));
					repoOrder.verifyNoMoreInteractions();

					then(createServiceInstanceWorkflow1).shouldHaveNoMoreInteractions();
					then(createServiceInstanceWorkflow2).shouldHaveNoMoreInteractions();

					assertThat(response).isNotNull();
				})
				.verifyComplete();
	}

	@Test
	void deleteServiceInstance() {
		given(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance started",
										new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.SUCCEEDED, "delete service instance completed",
										new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		DeleteServiceInstanceResponseBuilder responseBuilder = DeleteServiceInstanceResponse.builder();
		DeleteServiceInstanceResponse builtResponse = DeleteServiceInstanceResponse.builder()
				.async(true)
				.operation("working2")
				.build();

		TestPublisher<Void> lowerOrderFlow = TestPublisher.create();
		TestPublisher<Void> higherOrderFlow = TestPublisher.create();

		given(deleteServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(true));
		given(
				deleteServiceInstanceWorkflow1
						.buildResponse(eq(request), any(DeleteServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder
						.async(true)
						.operation("working1")));
		given(deleteServiceInstanceWorkflow1.delete(eq(request), eq(builtResponse)))
				.willReturn(lowerOrderFlow.mono());

		given(deleteServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(true));
		given(
				deleteServiceInstanceWorkflow2
						.buildResponse(eq(request), any(DeleteServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder
						.operation("working2")));
		given(deleteServiceInstanceWorkflow2.delete(eq(request), eq(builtResponse)))
				.willReturn(higherOrderFlow.mono());

		StepVerifier.create(workflowServiceInstanceService.deleteServiceInstance(request))
				.assertNext(response -> {
					InOrder repoOrder = inOrder(serviceInstanceStateRepository);
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.IN_PROGRESS),
									eq("delete service instance started"));
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.SUCCEEDED),
									eq("delete service instance completed"));
					repoOrder.verifyNoMoreInteractions();

					lowerOrderFlow.complete();
					lowerOrderFlow.assertWasNotRequested();

					higherOrderFlow.complete();
					lowerOrderFlow.assertWasRequested();

					InOrder deleteOrder = inOrder(deleteServiceInstanceWorkflow1, deleteServiceInstanceWorkflow2);
					deleteOrder.verify(deleteServiceInstanceWorkflow2).buildResponse(eq(request),
							any(DeleteServiceInstanceResponseBuilder.class));
					deleteOrder.verify(deleteServiceInstanceWorkflow1).buildResponse(eq(request),
							any(DeleteServiceInstanceResponseBuilder.class));
					deleteOrder.verify(deleteServiceInstanceWorkflow2).delete(request, responseBuilder.build());
					deleteOrder.verify(deleteServiceInstanceWorkflow1).delete(request, responseBuilder.build());
					deleteOrder.verifyNoMoreInteractions();

					assertThat(response).isNotNull();
					assertThat(response.isAsync()).isTrue();
					assertThat(response.getOperation()).isEqualTo("working2");
				})
				.verifyComplete();
	}

	@Test
	void deleteServiceInstanceWithAsyncError() {
		given(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance started",
										new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
				.willReturn(Mono.just(new ServiceInstanceState(OperationState.FAILED, "delete service instance failed",
						new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		DeleteServiceInstanceResponseBuilder responseBuilder = DeleteServiceInstanceResponse.builder();

		given(deleteServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(true));
		given(deleteServiceInstanceWorkflow1.delete(request, responseBuilder.build()))
				.willReturn(Mono.error(new RuntimeException("delete foo error")));
		given(
				deleteServiceInstanceWorkflow1
						.buildResponse(eq(request), any(DeleteServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder));

		given(deleteServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(true));
		given(deleteServiceInstanceWorkflow2.delete(request, responseBuilder.build()))
				.willReturn(Mono.empty());
		given(
				deleteServiceInstanceWorkflow2
						.buildResponse(eq(request), any(DeleteServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceService.deleteServiceInstance(request))
				.assertNext(response -> {
					InOrder repoOrder = inOrder(serviceInstanceStateRepository);
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.IN_PROGRESS),
									eq("delete service instance started"));
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.FAILED), eq("delete foo error"));
					repoOrder.verifyNoMoreInteractions();

					InOrder deleteOrder = inOrder(deleteServiceInstanceWorkflow1, deleteServiceInstanceWorkflow2);
					deleteOrder.verify(deleteServiceInstanceWorkflow2).buildResponse(eq(request),
							any(DeleteServiceInstanceResponseBuilder.class));
					deleteOrder.verify(deleteServiceInstanceWorkflow1).buildResponse(eq(request),
							any(DeleteServiceInstanceResponseBuilder.class));
					deleteOrder.verify(deleteServiceInstanceWorkflow2).delete(request, responseBuilder.build());
					deleteOrder.verify(deleteServiceInstanceWorkflow1).delete(request, responseBuilder.build());
					deleteOrder.verifyNoMoreInteractions();

					assertThat(response).isNotNull();
				})
				.verifyComplete();
	}

	@Test
	void deleteServiceInstanceWithResponseError() {
		DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		DeleteServiceInstanceResponseBuilder responseBuilder = DeleteServiceInstanceResponse.builder();

		given(deleteServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(true));
		given(
				deleteServiceInstanceWorkflow1
						.buildResponse(eq(request), any(DeleteServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.error(new ServiceBrokerException("delete foo error")));

		given(deleteServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(true));
		given(
				deleteServiceInstanceWorkflow2
						.buildResponse(eq(request), any(DeleteServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceService.deleteServiceInstance(request))
				.expectErrorSatisfies(e -> assertThat(e)
						.isInstanceOf(ServiceBrokerException.class)
						.hasMessage("delete foo error"))
				.verify();
	}

	@Test
	void deleteServiceInstanceWithNoAcceptsDoesNothing() {
		given(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance started",
										new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.SUCCEEDED, "delete service instance completed",
										new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		given(deleteServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(false));

		given(deleteServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(false));

		StepVerifier.create(workflowServiceInstanceService.deleteServiceInstance(request))
				.assertNext(response -> {
					InOrder repoOrder = inOrder(serviceInstanceStateRepository);
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.IN_PROGRESS),
									eq("delete service instance started"));
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.SUCCEEDED),
									eq("delete service instance completed"));
					repoOrder.verifyNoMoreInteractions();

					then(createServiceInstanceWorkflow1).shouldHaveNoMoreInteractions();
					then(createServiceInstanceWorkflow2).shouldHaveNoMoreInteractions();

					assertThat(response).isNotNull();
				})
				.verifyComplete();
	}

	@Test
	void updateServiceInstance() {
		given(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.IN_PROGRESS, "update service instance started",
										new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.SUCCEEDED, "update service instance completed",
										new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		UpdateServiceInstanceRequest request = UpdateServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		UpdateServiceInstanceResponseBuilder responseBuilder = UpdateServiceInstanceResponse.builder();
		UpdateServiceInstanceResponse builtResponse = UpdateServiceInstanceResponse.builder()
				.async(true)
				.dashboardUrl("https://dashboard.example.com")
				.operation("working2")
				.build();

		TestPublisher<Void> lowerOrderFlow = TestPublisher.create();
		TestPublisher<Void> higherOrderFlow = TestPublisher.create();

		given(updateServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(true));
		given(
				updateServiceInstanceWorkflow1
						.buildResponse(eq(request), any(UpdateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder
						.async(true)
						.operation("working1")));
		given(updateServiceInstanceWorkflow1.update(eq(request), eq(builtResponse)))
				.willReturn(lowerOrderFlow.mono());

		given(updateServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(true));
		given(
				updateServiceInstanceWorkflow2
						.buildResponse(eq(request), any(UpdateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder
						.dashboardUrl("https://dashboard.example.com")
						.operation("working2")));
		given(updateServiceInstanceWorkflow2.update(eq(request), eq(builtResponse)))
				.willReturn(higherOrderFlow.mono());

		StepVerifier.create(workflowServiceInstanceService.updateServiceInstance(request))
				.assertNext(response -> {
					InOrder repoOrder = inOrder(serviceInstanceStateRepository);
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.IN_PROGRESS),
									eq("update service instance started"));
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.SUCCEEDED),
									eq("update service instance completed"));
					repoOrder.verifyNoMoreInteractions();

					lowerOrderFlow.complete();
					lowerOrderFlow.assertWasNotRequested();

					higherOrderFlow.complete();
					lowerOrderFlow.assertWasRequested();

					InOrder updateOrder = inOrder(updateServiceInstanceWorkflow1, updateServiceInstanceWorkflow2);
					updateOrder.verify(updateServiceInstanceWorkflow2).buildResponse(eq(request),
							any(UpdateServiceInstanceResponseBuilder.class));
					updateOrder.verify(updateServiceInstanceWorkflow1).buildResponse(eq(request),
							any(UpdateServiceInstanceResponseBuilder.class));
					updateOrder.verify(updateServiceInstanceWorkflow2).update(request, builtResponse);
					updateOrder.verify(updateServiceInstanceWorkflow1).update(request, builtResponse);
					updateOrder.verifyNoMoreInteractions();

					assertThat(response).isNotNull();
					assertThat(response.isAsync()).isTrue();
					assertThat(response.getDashboardUrl()).isEqualTo("https://dashboard.example.com");
					assertThat(response.getOperation()).isEqualTo("working2");
				})
				.verifyComplete();
	}

	@Test
	void updateServiceInstanceWithAsyncError() {
		given(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.IN_PROGRESS, "update service instance started",
										new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
				.willReturn(Mono.just(new ServiceInstanceState(OperationState.FAILED, "update service instance failed",
						new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		UpdateServiceInstanceRequest request = UpdateServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		UpdateServiceInstanceResponseBuilder responseBuilder = UpdateServiceInstanceResponse.builder();

		given(updateServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(true));
		given(updateServiceInstanceWorkflow1.update(request, responseBuilder.build()))
				.willReturn(Mono.error(new RuntimeException("update foo error")));
		given(
				updateServiceInstanceWorkflow1
						.buildResponse(eq(request), any(UpdateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder));

		given(updateServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(true));
		given(updateServiceInstanceWorkflow2.update(request, responseBuilder.build()))
				.willReturn(Mono.empty());
		given(
				updateServiceInstanceWorkflow2
						.buildResponse(eq(request), any(UpdateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceService.updateServiceInstance(request))
				.assertNext(response -> {
					InOrder repoOrder = inOrder(serviceInstanceStateRepository);
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.IN_PROGRESS),
									eq("update service instance started"));
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.FAILED), eq("update foo error"));
					repoOrder.verifyNoMoreInteractions();

					InOrder updateOrder = inOrder(updateServiceInstanceWorkflow1, updateServiceInstanceWorkflow2);
					updateOrder.verify(updateServiceInstanceWorkflow2).buildResponse(eq(request),
							any(UpdateServiceInstanceResponseBuilder.class));
					updateOrder.verify(updateServiceInstanceWorkflow1).buildResponse(eq(request),
							any(UpdateServiceInstanceResponseBuilder.class));
					updateOrder.verify(updateServiceInstanceWorkflow2).update(request, responseBuilder.build());
					updateOrder.verify(updateServiceInstanceWorkflow1).update(request, responseBuilder.build());
					updateOrder.verifyNoMoreInteractions();

					assertThat(response).isNotNull();
				})
				.verifyComplete();
	}

	@Test
	void updateServiceInstanceWithResponseError() {
		UpdateServiceInstanceRequest request = UpdateServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		UpdateServiceInstanceResponseBuilder responseBuilder = UpdateServiceInstanceResponse.builder();

		given(updateServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(true));
		given(
				updateServiceInstanceWorkflow1
						.buildResponse(eq(request), any(UpdateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.error(new ServiceBrokerException("update foo error")));

		given(updateServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(true));
		given(
				updateServiceInstanceWorkflow2
						.buildResponse(eq(request), any(UpdateServiceInstanceResponseBuilder.class)))
				.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceService.updateServiceInstance(request))
				.expectErrorSatisfies(e -> assertThat(e)
						.isInstanceOf(ServiceBrokerException.class)
						.hasMessage("update foo error"))
				.verify();
	}

	@Test
	void updateServiceInstanceWithNoAcceptsDoesNothing() {
		given(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.IN_PROGRESS, "update service instance started",
										new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
				.willReturn(
						Mono.just(
								new ServiceInstanceState(OperationState.SUCCEEDED, "update service instance completed",
										new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

		UpdateServiceInstanceRequest request = UpdateServiceInstanceRequest.builder()
				.serviceInstanceId("foo")
				.build();

		given(updateServiceInstanceWorkflow1.accept(request))
				.willReturn(Mono.just(false));

		given(updateServiceInstanceWorkflow2.accept(request))
				.willReturn(Mono.just(false));

		StepVerifier.create(workflowServiceInstanceService.updateServiceInstance(request))
				.assertNext(response -> {
					InOrder repoOrder = inOrder(serviceInstanceStateRepository);
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.IN_PROGRESS),
									eq("update service instance started"));
					repoOrder.verify(serviceInstanceStateRepository)
							.saveState(eq("foo"), eq(OperationState.SUCCEEDED),
									eq("update service instance completed"));
					repoOrder.verifyNoMoreInteractions();

					then(updateServiceInstanceWorkflow1).shouldHaveNoMoreInteractions();
					then(updateServiceInstanceWorkflow2).shouldHaveNoMoreInteractions();

					assertThat(response).isNotNull();
				})
				.verifyComplete();
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	private interface HighOrderCreateServiceInstanceWorkflow extends CreateServiceInstanceWorkflow {
	}

	@Order
	private interface LowOrderCreateServiceInstanceWorkflow extends CreateServiceInstanceWorkflow {
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	private interface HighOrderDeleteServiceInstanceWorkflow extends DeleteServiceInstanceWorkflow {
	}

	@Order
	private interface LowOrderDeleteServiceInstanceWorkflow extends DeleteServiceInstanceWorkflow {
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	private interface HighOrderUpdateServiceInstanceWorkflow extends UpdateServiceInstanceWorkflow {
	}

	@Order
	private interface LowOrderUpdateServiceInstanceWorkflow extends UpdateServiceInstanceWorkflow {
	}

}
