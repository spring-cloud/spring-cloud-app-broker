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

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.workflow.instance.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;

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
//		return create(request)
//			.thenReturn(CreateServiceInstanceResponse.builder()
//				.async(true)
//				.build());
//			.doOnRequest(l -> log.info("Responding from create service instance {}", request))
//			.doOnSuccess(d -> log.info("Finished responding from create service instance {}", request))
//			.doOnError(e -> log.info("Error responding from create service instance {} with error {}", request, e));

		return Mono.just(CreateServiceInstanceResponse.builder()
			.async(true)
			.build())
			.publishOn(Schedulers.parallel())
			.doOnNext(response -> create(request));

	}

	private Mono<String> create(CreateServiceInstanceRequest request) {
		return stateRepository.saveState(request.getServiceInstanceId(),
			OperationState.IN_PROGRESS,
			"create service instance started")
			.then(createServiceInstanceWorkflow.create())
			.doOnRequest(l -> {
				log.info("Creating service instance {}", request);
			})
			.doOnSuccess(d -> {
				log.info("Finished creating service instance {}", request);
				stateRepository.saveState(request.getServiceInstanceId(),
					OperationState.SUCCEEDED,
					"create service instance completed");
			})
			.doOnError(e -> {
				log.info("Error creating service instance {} with error {}", request, e);
				stateRepository.saveState(request.getServiceInstanceId(),
					OperationState.FAILED,
					e.getMessage());
			});
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		//TODO add functionality
		return deleteServiceInstanceWorkflow.delete()
			.doOnRequest(l -> stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.IN_PROGRESS,
				"delete service instance started"))
			.doOnSuccess(d -> stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.SUCCEEDED,
				"delete service instance completed"))
			.doOnError(e -> stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.FAILED,
				e.getMessage()))
			.thenReturn(DeleteServiceInstanceResponse.builder()
				.async(true)
				.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		return stateRepository.getState(request.getServiceInstanceId())
			.doOnError(e -> Mono.error(new ServiceInstanceDoesNotExistException(request.getServiceInstanceId())))
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
}
