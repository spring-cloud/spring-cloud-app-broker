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

package org.springframework.cloud.appbroker.workflow;

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

public class WorkflowServiceInstanceService implements ServiceInstanceService {

	private CreateServiceInstanceWorkflow createServiceInstanceWorkflow;
	private DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow;
	private GetLastOperationWorkflow getLastOperationWorkflow;
	private GetServiceInstanceWorkflow getServiceInstanceWorkflow;
	private UpdateServiceInstanceWorkflow updateServiceInstanceWorkflow;


	public WorkflowServiceInstanceService(CreateServiceInstanceWorkflow createServiceInstanceWorkflow,
										  UpdateServiceInstanceWorkflow updateServiceInstanceWorkflow,
										  GetServiceInstanceWorkflow getServiceInstanceWorkflow,
										  GetLastOperationWorkflow getLastOperationWorkflow,
										  DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow) {

		this.createServiceInstanceWorkflow = createServiceInstanceWorkflow;
		this.updateServiceInstanceWorkflow = updateServiceInstanceWorkflow;
		this.getServiceInstanceWorkflow = getServiceInstanceWorkflow;
		this.getLastOperationWorkflow = getLastOperationWorkflow;
		this.deleteServiceInstanceWorkflow = deleteServiceInstanceWorkflow;
	}

	@Override
	public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
		return createServiceInstanceWorkflow.perform(request);
	}

	@Override
	public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
		return updateServiceInstanceWorkflow.perform(request);
	}

	@Override
	public GetServiceInstanceResponse getServiceInstance(GetServiceInstanceRequest request) {
		return getServiceInstanceWorkflow.perform(request);
	}

	@Override
	public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
		return getLastOperationWorkflow.perform(request);
	}

	@Override
	public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return deleteServiceInstanceWorkflow.perform(request);
	}
}
