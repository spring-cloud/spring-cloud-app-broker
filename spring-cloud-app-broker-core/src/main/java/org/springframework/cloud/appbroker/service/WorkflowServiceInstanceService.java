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

import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.List;

/**
 * A {@code ServiceInstanceService} that delegates to a set of discrete Workflow objects for each service broker
 * operation.
 */
public class WorkflowServiceInstanceService implements ServiceInstanceService {
	private final Logger log = Loggers.getLogger(WorkflowServiceInstanceService.class);

	private List<CreateServiceInstanceWorkflow> createServiceInstanceWorkflows;

	private List<DeleteServiceInstanceWorkflow> deleteServiceInstanceWorkflows;

	private List<UpdateServiceInstanceWorkflow> updateServiceInstanceWorkflows;

	private ServiceInstanceStateRepository stateRepository;

	public WorkflowServiceInstanceService(ServiceInstanceStateRepository serviceInstanceStateRepository,
										  List<CreateServiceInstanceWorkflow> createServiceInstanceWorkflows,
										  List<DeleteServiceInstanceWorkflow> deleteServiceInstanceWorkflows,
										  List<UpdateServiceInstanceWorkflow> updateServiceInstanceWorkflows) {
		this.stateRepository = serviceInstanceStateRepository;
		this.createServiceInstanceWorkflows = createServiceInstanceWorkflows;
		this.deleteServiceInstanceWorkflows = deleteServiceInstanceWorkflows;
		this.updateServiceInstanceWorkflows = updateServiceInstanceWorkflows;
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		return Mono.just(CreateServiceInstanceResponse.builder()
			.async(true)
			.build())
			.publishOn(Schedulers.parallel())
			.doOnNext(response -> create(request)
				.subscribe());
	}

	private Mono<Void> create(CreateServiceInstanceRequest request) {
		return stateRepository.saveState(request.getServiceInstanceId(),
			OperationState.IN_PROGRESS,
			"create service instance started")
			.then(invokeCreateWorkflows(request)
				.doOnRequest(l -> log.info("Creating service instance {}", request))
				.doOnSuccess(d -> log.info("Finished creating service instance {}", request))
				.doOnError(e -> log.info("Error creating service instance {} with error {}", request, e)))
			.thenEmpty(stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.SUCCEEDED, "create service instance completed")
				.then())
			.onErrorResume(e -> stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.FAILED, e.getMessage())
				.then());
	}

	private Mono<Void> invokeCreateWorkflows(CreateServiceInstanceRequest request) {
		return Flux.fromIterable(createServiceInstanceWorkflows)
			.flatMap(createServiceInstanceWorkflow -> createServiceInstanceWorkflow.create(request))
			.thenEmpty(Mono.empty());
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return Mono.just(DeleteServiceInstanceResponse.builder()
			.async(true)
			.build())
			.publishOn(Schedulers.parallel())
			.doOnNext(response -> delete(request)
				.subscribe());
	}

	private Mono<Void> delete(DeleteServiceInstanceRequest request) {
		return stateRepository.saveState(request.getServiceInstanceId(),
			OperationState.IN_PROGRESS, "delete service instance started")
			.then(invokeDeleteWorkflows(request)
				.doOnRequest(l -> log.info("Deleting service instance {}", request))
				.doOnSuccess(d -> log.info("Finished deleting service instance {}", request))
				.doOnError(e -> log.info("Error deleting service instance {} with error {}", request, e)))
			.thenEmpty(stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.SUCCEEDED, "delete service instance completed")
				.then())
			.onErrorResume(e -> stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.FAILED, e.getMessage())
				.then());
	}

	private Mono<Void> invokeDeleteWorkflows(DeleteServiceInstanceRequest request) {
		return Flux.fromIterable(deleteServiceInstanceWorkflows)
			.flatMap(deleteServiceInstanceWorkflow -> deleteServiceInstanceWorkflow.delete(request))
			.thenEmpty(Mono.empty());
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
		return Mono.just(UpdateServiceInstanceResponse.builder()
			.async(true)
			.build())
			.publishOn(Schedulers.parallel())
			.doOnNext(response -> update(request)
				.subscribe());
	}

	private Mono<Void> update(UpdateServiceInstanceRequest request) {
		return stateRepository.saveState(request.getServiceInstanceId(),
			OperationState.IN_PROGRESS, "update service instance started")
			.then(invokeUpdateWorkflows(request)
				.doOnRequest(l -> log.info("Updating service instance {}", request))
				.doOnSuccess(d -> log.info("Finished updating service instance {}", request))
				.doOnError(e -> log.info("Error updating service instance {} with error {}", request, e)))
			.thenEmpty(stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.SUCCEEDED, "update service instance completed")
				.then())
			.onErrorResume(e -> stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.FAILED, e.getMessage())
				.then());
	}

	private Mono<Void> invokeUpdateWorkflows(UpdateServiceInstanceRequest request) {
		return Flux.fromIterable(updateServiceInstanceWorkflows)
			.flatMap(updateServiceInstanceWorkflow -> updateServiceInstanceWorkflow.update(request))
			.thenEmpty(Mono.empty());
	}
}
