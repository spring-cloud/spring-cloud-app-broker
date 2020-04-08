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
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import org.springframework.cloud.appbroker.state.ServiceInstanceBindingStateRepository;
import org.springframework.cloud.appbroker.state.ServiceInstanceState;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceInstanceSyncBindingServiceTest {

	@Mock
	private ServiceInstanceBindingStateRepository stateRepository;

	private WorkflowServiceInstanceBindingService workflowServiceInstanceBindingService;

	@BeforeEach
	void setUp() {
		given(stateRepository.saveState(anyString(), anyString(), any(OperationState.class), anyString()))
			.willReturn(Mono.just(
				new ServiceInstanceState(OperationState.IN_PROGRESS, "create service instance binding started",
					new Timestamp(Instant.now().minusSeconds(60).toEpochMilli()))))
			.willReturn(Mono.just(
				new ServiceInstanceState(OperationState.SUCCEEDED, "create service instance binding completed",
					new Timestamp(Instant.now().minusSeconds(30).toEpochMilli()))));

	}

	@Test
	void synchronouslyCreateServiceInstanceAppBinding() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder()
			.serviceInstanceId("foo-service")
			.bindingId("foo-binding")
			.bindResource(BindResource.builder()
				.appGuid("foo-guid")
				.build())
			.build();

		TestPublisher<Void> publisher = TestPublisher.create();
		CreateServiceInstanceAppBindingWorkflow test = new CreateServiceInstanceAppBindingWorkflow() {
			@Override
			public Mono<Void> create(CreateServiceInstanceBindingRequest request,
				CreateServiceInstanceAppBindingResponse response) {
				return publisher.mono();
			}
		};

		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(this.stateRepository,
			Collections.singletonList(test), Collections.emptyList(), Collections.emptyList());

		StepVerifier.create(this.workflowServiceInstanceBindingService.createServiceInstanceBinding(request))
			.expectSubscription()
			.expectNoEvent(Duration.ofSeconds(1))
			.then(publisher::complete)
			.expectNextCount(1)
			.expectComplete()
			.verify();
	}

	@Test
	void synchronouslyDeleteServiceInstanceAppBinding() {
		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest.builder()
			.serviceInstanceId("test-si")
			.bindingId("test-binding")
			.build();

		TestPublisher<Void> publisher = TestPublisher.create();
		DeleteServiceInstanceBindingWorkflow test = new DeleteServiceInstanceBindingWorkflow() {
			@Override
			public Mono<Void> delete(DeleteServiceInstanceBindingRequest request,
				DeleteServiceInstanceBindingResponse response) {
				return publisher.mono();
			}
		};

		this.workflowServiceInstanceBindingService = new WorkflowServiceInstanceBindingService(this.stateRepository,
			Collections.emptyList(), Collections.emptyList(), Collections.singletonList(test));

		StepVerifier.create(this.workflowServiceInstanceBindingService.deleteServiceInstanceBinding(request))
			.expectSubscription()
			.expectNoEvent(Duration.ofSeconds(1))
			.then(publisher::complete)
			.expectNextCount(1)
			.expectComplete()
			.verify();
	}



}
