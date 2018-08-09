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

import org.springframework.cloud.servicebroker.model.instance.OperationState;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
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
import reactor.util.Logger;
import reactor.util.Loggers;

/**
 * A {@code ServiceInstanceService} that delegates to a set of discrete Workflow objects for each service broker
 * operation.
 */
public class WorkflowServiceInstanceService implements ServiceInstanceService {
	private final Logger log = Loggers.getLogger(WorkflowServiceInstanceService.class);

	private CreateServiceInstanceWorkflow createServiceInstanceWorkflow;

	private DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow;

	private ServiceInstanceStateRepository stateRepository;

	public WorkflowServiceInstanceService(ServiceInstanceStateRepository serviceInstanceStateRepository,
										  CreateServiceInstanceWorkflow createServiceInstanceWorkflow,
										  DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow) {
		this.stateRepository = serviceInstanceStateRepository;
		this.createServiceInstanceWorkflow = createServiceInstanceWorkflow;
		this.deleteServiceInstanceWorkflow = deleteServiceInstanceWorkflow;
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		//TODO add functionality
		return createServiceInstanceWorkflow.create()
			.doOnRequest(l -> log.info("Creating service instance {}", request))
			.doOnSuccess(d -> log.info("Finished creating service instance {}", request))
			.doOnError(e -> log.info("Error creating service instance {} with error {}", request, e))
			.thenReturn(CreateServiceInstanceResponse.builder()
				.async(true)
				.build())
			.doOnRequest(l -> log.info("Responding from create service instance {}", request))
			.doOnSuccess(d -> log.info("Finished responding from create service instance {}", request))
			.doOnError(e -> log.info("Error responding from create service instance {} with error {}", request, e));
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		//TODO add functionality
		return deleteServiceInstanceWorkflow.delete()
			.thenReturn(DeleteServiceInstanceResponse.builder()
				.async(true)
				.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		return stateRepository.getState(request.getServiceInstanceId())
			.map(serviceInstanceState -> GetLastServiceOperationResponse.builder()
				.operationState(serviceInstanceState.getOperationState())
				.description(serviceInstanceState.getDescription())
				.build());
	}

	@Override
	public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
		//TODO add functionality
		return Mono.empty();
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		//TODO add functionality
		return Mono.empty();
	}

	@Override
	public Mono<Void> getBeforeCreateFlow(CreateServiceInstanceRequest request) {
		return stateRepository.saveState(request.getServiceInstanceId(), OperationState.IN_PROGRESS, "create service instance")
			.then();
	}

	@Override
	public Mono<Void> getAfterCreateFlow(CreateServiceInstanceRequest request,
										 CreateServiceInstanceResponse response) {
		return stateRepository.saveState(request.getServiceInstanceId(), OperationState.SUCCEEDED, response.toString())
			.then();
	}

	@Override
	public Mono<Void> getErrorCreateFlow(CreateServiceInstanceRequest request,
										 Throwable error) {
		return stateRepository.saveState(request.getServiceInstanceId(), OperationState.FAILED, error.getMessage())
			.then();
	}

	@Override
	public Mono<Void> getBeforeDeleteFlow(DeleteServiceInstanceRequest request) {
		return stateRepository.saveState(request.getServiceInstanceId(), OperationState.IN_PROGRESS, "delete service instance")
			.then();
	}

	@Override
	public Mono<Void> getAfterDeleteFlow(DeleteServiceInstanceRequest request, DeleteServiceInstanceResponse response) {
		return stateRepository.saveState(request.getServiceInstanceId(), OperationState.SUCCEEDED, response.toString())
			.then();
	}

	@Override
	public Mono<Void> getErrorDeleteFlow(DeleteServiceInstanceRequest request, Throwable error) {
		return stateRepository.saveState(request.getServiceInstanceId(), OperationState.FAILED, error.getMessage())
			.then();
	}

	@Override
	public Mono<Void> getBeforeUpdateFlow(UpdateServiceInstanceRequest request) {
		return stateRepository.saveState(request.getServiceInstanceId(), OperationState.IN_PROGRESS, "update service instance")
			.then();
	}

	@Override
	public Mono<Void> getAfterUpdateFlow(UpdateServiceInstanceRequest request, UpdateServiceInstanceResponse response) {
		return stateRepository.saveState(request.getServiceInstanceId(), OperationState.SUCCEEDED, response.toString())
			.then();
	}

	@Override
	public Mono<Void> getErrorUpdateFlow(UpdateServiceInstanceRequest request, Throwable error) {
		return stateRepository.saveState(request.getServiceInstanceId(), OperationState.FAILED, error.getMessage())
			.then();
	}
}
