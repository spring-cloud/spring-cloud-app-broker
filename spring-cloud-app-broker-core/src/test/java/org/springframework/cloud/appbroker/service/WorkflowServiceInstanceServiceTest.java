/*
 * Copyright 2016-2018 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.state.ServiceInstanceState;
import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse.DeleteServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse.UpdateServiceInstanceResponseBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

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
		when(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance started")))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance completed")));

		CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
			.serviceInstanceId("foo")
			.build();

		CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();

		given(createServiceInstanceWorkflow1.create(request))
			.willReturn(Flux.empty());
		given(createServiceInstanceWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.async(true)
				.operation("working1")));

		given(createServiceInstanceWorkflow2.create(request))
			.willReturn(Flux.empty());
		given(createServiceInstanceWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.dashboardUrl("https://dashboard.example.com")
				.operation("working2")));

		StepVerifier.create(workflowServiceInstanceService.createServiceInstance(request))
			.assertNext(createServiceInstanceResponse -> {
				InOrder repoOrder = inOrder(serviceInstanceStateRepository);
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("create service instance started"));
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.SUCCEEDED), eq("create service instance completed"));
				repoOrder.verifyNoMoreInteractions();

				InOrder createOrder = inOrder(createServiceInstanceWorkflow1, createServiceInstanceWorkflow2);
				createOrder.verify(createServiceInstanceWorkflow2).buildResponse(eq(request),
					any(CreateServiceInstanceResponseBuilder.class));
				createOrder.verify(createServiceInstanceWorkflow1).buildResponse(eq(request),
					any(CreateServiceInstanceResponseBuilder.class));
				createOrder.verify(createServiceInstanceWorkflow2).create(request);
				createOrder.verify(createServiceInstanceWorkflow1).create(request);
				createOrder.verifyNoMoreInteractions();

				assertThat(createServiceInstanceResponse).isNotNull();
				assertThat(createServiceInstanceResponse.isAsync()).isTrue();
				assertThat(createServiceInstanceResponse.getDashboardUrl()).isEqualTo("https://dashboard.example.com");
				assertThat(createServiceInstanceResponse.getOperation()).isEqualTo("working2");
			})
			.verifyComplete();
	}

	@Test
	void createServiceInstanceError() {
		when(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance started")))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.FAILED, "create service instance failed")));

		CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
			.serviceInstanceId("foo")
			.build();

		CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();

		given(createServiceInstanceWorkflow1.create(request))
			.willReturn(Flux.error(new RuntimeException("create foo error")));
		given(createServiceInstanceWorkflow1.buildResponse(eq(request), any(CreateServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(createServiceInstanceWorkflow2.create(request))
			.willReturn(Flux.empty());
		given(createServiceInstanceWorkflow2.buildResponse(eq(request), any(CreateServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceService.createServiceInstance(request))
			.assertNext(error -> {
				InOrder repoOrder = inOrder(serviceInstanceStateRepository);
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("create service instance started"));
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.FAILED), eq("create foo error"));
				repoOrder.verifyNoMoreInteractions();

				InOrder createOrder = inOrder(createServiceInstanceWorkflow1, createServiceInstanceWorkflow2);
				createOrder.verify(createServiceInstanceWorkflow2).buildResponse(eq(request),
					any(CreateServiceInstanceResponseBuilder.class));
				createOrder.verify(createServiceInstanceWorkflow1).buildResponse(eq(request),
					any(CreateServiceInstanceResponseBuilder.class));
				createOrder.verify(createServiceInstanceWorkflow2).create(request);
				createOrder.verify(createServiceInstanceWorkflow1).create(request);
				createOrder.verifyNoMoreInteractions();
			})
			.verifyComplete();
	}

	@Test
	void deleteServiceInstance() {
		when(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance started")))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "delete service instance completed")));

		DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
			.serviceInstanceId("foo")
			.build();

		DeleteServiceInstanceResponseBuilder responseBuilder = DeleteServiceInstanceResponse.builder();

		given(deleteServiceInstanceWorkflow1.delete(request))
			.willReturn(Flux.empty());
		given(deleteServiceInstanceWorkflow1.buildResponse(eq(request), any(DeleteServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.async(true)
				.operation("working1")));

		given(deleteServiceInstanceWorkflow2.delete(request))
			.willReturn(Flux.empty());
		given(deleteServiceInstanceWorkflow2.buildResponse(eq(request), any(DeleteServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.operation("working2")));

		StepVerifier.create(workflowServiceInstanceService.deleteServiceInstance(request))
			.consumeNextWith(deleteServiceInstanceResponse -> {
				InOrder repoOrder = inOrder(serviceInstanceStateRepository);
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("delete service instance started"));
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.SUCCEEDED), eq("delete service instance completed"));
				repoOrder.verifyNoMoreInteractions();

				InOrder deleteOrder = inOrder(deleteServiceInstanceWorkflow1, deleteServiceInstanceWorkflow2);
				deleteOrder.verify(deleteServiceInstanceWorkflow2).buildResponse(eq(request),
					any(DeleteServiceInstanceResponseBuilder.class));
				deleteOrder.verify(deleteServiceInstanceWorkflow1).buildResponse(eq(request),
					any(DeleteServiceInstanceResponseBuilder.class));
				deleteOrder.verify(deleteServiceInstanceWorkflow2).delete(request);
				deleteOrder.verify(deleteServiceInstanceWorkflow1).delete(request);
				deleteOrder.verifyNoMoreInteractions();

				assertThat(deleteServiceInstanceResponse).isNotNull();
				assertThat(deleteServiceInstanceResponse.isAsync()).isTrue();
				assertThat(deleteServiceInstanceResponse.getOperation()).isEqualTo("working2");
			})
			.verifyComplete();
	}

	@Test
	void deleteServiceInstanceError() {
		when(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "delete service instance started")))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.FAILED, "delete service instance failed")));

		DeleteServiceInstanceRequest request = DeleteServiceInstanceRequest.builder()
			.serviceInstanceId("foo")
			.build();

		DeleteServiceInstanceResponseBuilder responseBuilder = DeleteServiceInstanceResponse.builder();

		given(deleteServiceInstanceWorkflow1.delete(request))
			.willReturn(Flux.error(new RuntimeException("delete foo error")));
		given(deleteServiceInstanceWorkflow1.buildResponse(eq(request), any(DeleteServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(deleteServiceInstanceWorkflow2.delete(request))
			.willReturn(Flux.empty());
		given(deleteServiceInstanceWorkflow2.buildResponse(eq(request), any(DeleteServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceService.deleteServiceInstance(request))
			.consumeNextWith(deleteServiceInstanceResponse -> {
				InOrder repoOrder = inOrder(serviceInstanceStateRepository);
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("delete service instance started"));
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.FAILED), eq("delete foo error"));
				repoOrder.verifyNoMoreInteractions();

				InOrder deleteOrder = inOrder(deleteServiceInstanceWorkflow1, deleteServiceInstanceWorkflow2);
				deleteOrder.verify(deleteServiceInstanceWorkflow2).buildResponse(eq(request),
					any(DeleteServiceInstanceResponseBuilder.class));
				deleteOrder.verify(deleteServiceInstanceWorkflow1).buildResponse(eq(request),
					any(DeleteServiceInstanceResponseBuilder.class));
				deleteOrder.verify(deleteServiceInstanceWorkflow2).delete(request);
				deleteOrder.verify(deleteServiceInstanceWorkflow1).delete(request);
				deleteOrder.verifyNoMoreInteractions();

				assertThat(deleteServiceInstanceResponse).isNotNull();
			})
			.verifyComplete();
	}

	@Test
	void updateServiceInstance() {
		when(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "update service instance started")))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.SUCCEEDED, "update service instance completed")));

		UpdateServiceInstanceRequest request = UpdateServiceInstanceRequest.builder()
			.serviceInstanceId("foo")
			.build();

		UpdateServiceInstanceResponseBuilder responseBuilder = UpdateServiceInstanceResponse.builder();

		given(updateServiceInstanceWorkflow1.update(request))
			.willReturn(Flux.empty());
		given(updateServiceInstanceWorkflow1.buildResponse(eq(request), any(UpdateServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.async(true)
				.operation("working1")));

		given(updateServiceInstanceWorkflow2.update(request))
			.willReturn(Flux.empty());
		given(updateServiceInstanceWorkflow2.buildResponse(eq(request), any(UpdateServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder
				.dashboardUrl("https://dashboard.example.com")
				.operation("working2")));

		StepVerifier.create(workflowServiceInstanceService.updateServiceInstance(request))
			.assertNext(updateServiceInstanceResponse -> {
				InOrder repoOrder = inOrder(serviceInstanceStateRepository);
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("update service instance started"));
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.SUCCEEDED), eq("update service instance completed"));
				repoOrder.verifyNoMoreInteractions();

				InOrder updateOrder = inOrder(updateServiceInstanceWorkflow1, updateServiceInstanceWorkflow2);
				updateOrder.verify(updateServiceInstanceWorkflow2).buildResponse(eq(request),
					any(UpdateServiceInstanceResponseBuilder.class));
				updateOrder.verify(updateServiceInstanceWorkflow1).buildResponse(eq(request),
					any(UpdateServiceInstanceResponseBuilder.class));
				updateOrder.verify(updateServiceInstanceWorkflow2).update(request);
				updateOrder.verify(updateServiceInstanceWorkflow1).update(request);
				updateOrder.verifyNoMoreInteractions();

				assertThat(updateServiceInstanceResponse).isNotNull();
				assertThat(updateServiceInstanceResponse.isAsync()).isTrue();
				assertThat(updateServiceInstanceResponse.getDashboardUrl()).isEqualTo("https://dashboard.example.com");
				assertThat(updateServiceInstanceResponse.getOperation()).isEqualTo("working2");
			})
			.verifyComplete();
	}

	@Test
	void updateServiceInstanceError() {
		when(serviceInstanceStateRepository.saveState(anyString(), any(OperationState.class), anyString()))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.IN_PROGRESS, "update service instance started")))
			.thenReturn(Mono.just(new ServiceInstanceState(OperationState.FAILED, "update service instance failed")));

		UpdateServiceInstanceRequest request = UpdateServiceInstanceRequest.builder()
			.serviceInstanceId("foo")
			.build();

		UpdateServiceInstanceResponseBuilder responseBuilder = UpdateServiceInstanceResponse.builder();

		given(updateServiceInstanceWorkflow1.update(request))
			.willReturn(Flux.error(new RuntimeException("update foo error")));
		given(updateServiceInstanceWorkflow1.buildResponse(eq(request), any(UpdateServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		given(updateServiceInstanceWorkflow2.update(request))
			.willReturn(Flux.empty());
		given(updateServiceInstanceWorkflow2.buildResponse(eq(request), any(UpdateServiceInstanceResponseBuilder.class)))
			.willReturn(Mono.just(responseBuilder));

		StepVerifier.create(workflowServiceInstanceService.updateServiceInstance(request))
			.assertNext(error -> {
				InOrder repoOrder = inOrder(serviceInstanceStateRepository);
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("update service instance started"));
				repoOrder.verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.FAILED), eq("update foo error"));
				repoOrder.verifyNoMoreInteractions();

				InOrder updateOrder = inOrder(updateServiceInstanceWorkflow1, updateServiceInstanceWorkflow2);
				updateOrder.verify(updateServiceInstanceWorkflow2).buildResponse(eq(request),
					any(UpdateServiceInstanceResponseBuilder.class));
				updateOrder.verify(updateServiceInstanceWorkflow1).buildResponse(eq(request),
					any(UpdateServiceInstanceResponseBuilder.class));
				updateOrder.verify(updateServiceInstanceWorkflow2).update(request);
				updateOrder.verify(updateServiceInstanceWorkflow1).update(request);
				updateOrder.verifyNoMoreInteractions();
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