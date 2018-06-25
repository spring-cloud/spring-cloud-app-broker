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

import org.springframework.cloud.appbroker.workflow.instance.CreateServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceInstanceServiceTest {

	@Mock
	private CreateServiceInstanceWorkflow createServiceInstanceWorkflow;
	private WorkflowServiceInstanceService workflowServiceInstanceService;

	@BeforeEach
	void setUp() {
		workflowServiceInstanceService = new WorkflowServiceInstanceService(createServiceInstanceWorkflow);
	}

	@Test
	void shouldProvisionServiceInstance() {
		// when we get a request to provision a service instance
		CreateServiceInstanceResponse createServiceInstanceResponse = workflowServiceInstanceService.createServiceInstance(null);

		// then we should delegate in the default workflow
		verify(createServiceInstanceWorkflow, times(1)).provision();

		// and then it should return a valid response with the last status
		assertThat(createServiceInstanceResponse).isNotNull();
	}
}