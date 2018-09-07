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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.state.ServiceInstanceState;
import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceInstanceServiceTest {

	@Mock
	private ServiceInstanceStateRepository serviceInstanceStateRepository;

	@Mock
	private CreateServiceInstanceWorkflow createServiceInstanceWorkflow1;

	@Mock
	private CreateServiceInstanceWorkflow createServiceInstanceWorkflow2;

	@Mock
	private DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow1;

	@Mock
	private DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow2;

	@Mock
	private UpdateServiceInstanceWorkflow updateServiceInstanceWorkflow1;

	@Mock
	private UpdateServiceInstanceWorkflow updateServiceInstanceWorkflow2;

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

		given(createServiceInstanceWorkflow1.create(request))
			.willReturn(Mono.empty());
		given(createServiceInstanceWorkflow2.create(request))
			.willReturn(Mono.empty());

		StepVerifier.create(workflowServiceInstanceService.createServiceInstance(request))
			.assertNext(createServiceInstanceResponse -> {
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("create service instance started"));
				verify(createServiceInstanceWorkflow1).create(request);
				verify(createServiceInstanceWorkflow2).create(request);
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.SUCCEEDED), eq("create service instance completed"));
				verifyNoMoreInteractions(serviceInstanceStateRepository,
					createServiceInstanceWorkflow1,
					createServiceInstanceWorkflow2);
				assertThat(createServiceInstanceResponse).isNotNull();
				assertThat(createServiceInstanceResponse.isAsync()).isTrue();
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

		given(createServiceInstanceWorkflow1.create(request))
			.willReturn(Mono.error(new RuntimeException("create foo error")));

		StepVerifier.create(workflowServiceInstanceService.createServiceInstance(request))
			.assertNext(error -> {
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("create service instance started"));
				verify(createServiceInstanceWorkflow1).create(request);
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.FAILED), eq("create foo error"));
				verifyNoMoreInteractions(serviceInstanceStateRepository,
					createServiceInstanceWorkflow1,
					createServiceInstanceWorkflow2);
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

		given(deleteServiceInstanceWorkflow1.delete(request))
			.willReturn(Mono.empty());
		given(deleteServiceInstanceWorkflow2.delete(request))
			.willReturn(Mono.empty());

		StepVerifier.create(workflowServiceInstanceService.deleteServiceInstance(request))
			.consumeNextWith(deleteServiceInstanceResponse -> {
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("delete service instance started"));
				verify(deleteServiceInstanceWorkflow1).delete(request);
				verify(deleteServiceInstanceWorkflow2).delete(request);
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.SUCCEEDED), eq("delete service instance completed"));
				verifyNoMoreInteractions(serviceInstanceStateRepository,
					deleteServiceInstanceWorkflow1,
					deleteServiceInstanceWorkflow2);

				assertThat(deleteServiceInstanceResponse).isNotNull();
				assertThat(deleteServiceInstanceResponse.isAsync()).isTrue();
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

		given(deleteServiceInstanceWorkflow1.delete(request))
			.willReturn(Mono.error(new RuntimeException("delete foo error")));

		StepVerifier.create(workflowServiceInstanceService.deleteServiceInstance(request))
			.consumeNextWith(deleteServiceInstanceResponse -> {
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("delete service instance started"));
				verify(deleteServiceInstanceWorkflow1).delete(request);
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.FAILED), eq("delete foo error"));
				verifyNoMoreInteractions(serviceInstanceStateRepository,
					deleteServiceInstanceWorkflow1,
					deleteServiceInstanceWorkflow2);

				assertThat(deleteServiceInstanceResponse).isNotNull();
				assertThat(deleteServiceInstanceResponse.isAsync()).isTrue();
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

		given(updateServiceInstanceWorkflow1.update(request))
			.willReturn(Mono.empty());
		given(updateServiceInstanceWorkflow2.update(request))
			.willReturn(Mono.empty());

		StepVerifier.create(workflowServiceInstanceService.updateServiceInstance(request))
			.assertNext(updateServiceInstanceResponse -> {
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("update service instance started"));
				verify(updateServiceInstanceWorkflow1).update(request);
				verify(updateServiceInstanceWorkflow2).update(request);
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.SUCCEEDED), eq("update service instance completed"));
				verifyNoMoreInteractions(serviceInstanceStateRepository,
					createServiceInstanceWorkflow1,
					createServiceInstanceWorkflow2);
				assertThat(updateServiceInstanceResponse).isNotNull();
				assertThat(updateServiceInstanceResponse.isAsync()).isTrue();
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

		given(updateServiceInstanceWorkflow1.update(request))
			.willReturn(Mono.error(new RuntimeException("update foo error")));

		StepVerifier.create(workflowServiceInstanceService.updateServiceInstance(request))
			.assertNext(error -> {
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.IN_PROGRESS), eq("update service instance started"));
				verify(updateServiceInstanceWorkflow1).update(request);
				verify(serviceInstanceStateRepository)
					.saveState(eq("foo"), eq(OperationState.FAILED), eq("update foo error"));
				verifyNoMoreInteractions(serviceInstanceStateRepository,
					createServiceInstanceWorkflow1,
					createServiceInstanceWorkflow2);
			})
			.verifyComplete();
	}
}