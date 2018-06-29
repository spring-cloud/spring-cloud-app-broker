/*
 * Copyright 2016-2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package org.springframework.cloud.appbroker.service;

import org.springframework.cloud.appbroker.workflow.instance.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;

/**
 * A {@code ServiceInstanceService} that delegates to a set of discrete Workflow objects for each service broker
 * operation.
 */
public class WorkflowServiceInstanceService implements ServiceInstanceService {

	private CreateServiceInstanceWorkflow createServiceInstanceWorkflow;
	private DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow;

	public WorkflowServiceInstanceService(CreateServiceInstanceWorkflow createServiceInstanceWorkflow,
										  DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow) {
		this.createServiceInstanceWorkflow = createServiceInstanceWorkflow;
		this.deleteServiceInstanceWorkflow = deleteServiceInstanceWorkflow;
	}

	@Override
	public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
		createServiceInstanceWorkflow.create();

		return CreateServiceInstanceResponse.builder().build();
	}

	@Override
	public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
		deleteServiceInstanceWorkflow.delete();
		
		return DeleteServiceInstanceResponse.builder().build();
	}

	@Override
	public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
		return null;
	}

	@Override
	public GetServiceInstanceResponse getServiceInstance(GetServiceInstanceRequest request) {
		return null;
	}

	@Override
	public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
		return null;
	}
}
