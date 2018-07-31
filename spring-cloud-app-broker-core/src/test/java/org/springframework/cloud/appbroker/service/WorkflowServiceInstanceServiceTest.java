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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.event.ServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.workflow.instance.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceInstanceServiceTest {

	@Mock
	private ServiceInstanceStateRepository serviceInstanceStateRepository;

	@Mock
	private CreateServiceInstanceWorkflow createServiceInstanceWorkflow;

	@Mock
	private DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow;

	private WorkflowServiceInstanceService workflowServiceInstanceService;

	@BeforeEach
	void setUp() {
		workflowServiceInstanceService = new WorkflowServiceInstanceService(serviceInstanceStateRepository,
			createServiceInstanceWorkflow, deleteServiceInstanceWorkflow);
	}

	@Test
	void shouldCreateServiceInstance() {
		given(createServiceInstanceWorkflow.create())
			.willReturn(Mono.empty());

		StepVerifier.create(workflowServiceInstanceService.createServiceInstance(CreateServiceInstanceRequest.builder()
			.serviceInstanceId("foo")
			.build()))
			.consumeNextWith(createServiceInstanceResponse -> {
				verify(createServiceInstanceWorkflow).create();
				assertThat(createServiceInstanceResponse).isNotNull();
				assertThat(createServiceInstanceResponse.isAsync()).isFalse();
			})
			.verifyComplete();
	}

	@Test
	void shouldDeleteServiceInstance() {
		given(deleteServiceInstanceWorkflow.delete())
			.willReturn(Mono.empty());

		StepVerifier.create(workflowServiceInstanceService.deleteServiceInstance(DeleteServiceInstanceRequest.builder()
			.serviceInstanceId("foo")
			.build()))
			.consumeNextWith(deleteServiceInstanceResponse -> {
				verify(deleteServiceInstanceWorkflow).delete();
				assertThat(deleteServiceInstanceResponse).isNotNull();
				assertThat(deleteServiceInstanceResponse.isAsync()).isFalse();
			})
			.verifyComplete();
	}
}