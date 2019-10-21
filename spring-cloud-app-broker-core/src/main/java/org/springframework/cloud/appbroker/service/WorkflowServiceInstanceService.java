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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse.DeleteServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse.UpdateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * A {@code ServiceInstanceService} that delegates to a set of discrete Workflow objects for each service broker
 * operation.
 */
public class WorkflowServiceInstanceService implements ServiceInstanceService {

	private final Logger log = Loggers.getLogger(WorkflowServiceInstanceService.class);

	private final List<CreateServiceInstanceWorkflow> createServiceInstanceWorkflows;

	private final List<DeleteServiceInstanceWorkflow> deleteServiceInstanceWorkflows;

	private final List<UpdateServiceInstanceWorkflow> updateServiceInstanceWorkflows;

	private final ServiceInstanceStateRepository stateRepository;

	public WorkflowServiceInstanceService(ServiceInstanceStateRepository serviceInstanceStateRepository,
										  List<CreateServiceInstanceWorkflow> createServiceInstanceWorkflows,
										  List<DeleteServiceInstanceWorkflow> deleteServiceInstanceWorkflows,
										  List<UpdateServiceInstanceWorkflow> updateServiceInstanceWorkflows) {
		this.stateRepository = serviceInstanceStateRepository;
		this.createServiceInstanceWorkflows = createServiceInstanceWorkflows;
		this.deleteServiceInstanceWorkflows = deleteServiceInstanceWorkflows;
		this.updateServiceInstanceWorkflows = updateServiceInstanceWorkflows;

		AnnotationAwareOrderComparator.sort(this.createServiceInstanceWorkflows);
		AnnotationAwareOrderComparator.sort(this.deleteServiceInstanceWorkflows);
		AnnotationAwareOrderComparator.sort(this.updateServiceInstanceWorkflows);
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		return invokeCreateResponseBuilders(request)
			.doOnNext(response -> create(request, response)
				.subscribe());
	}

	private Mono<CreateServiceInstanceResponse> invokeCreateResponseBuilders(CreateServiceInstanceRequest request) {
		AtomicReference<CreateServiceInstanceResponseBuilder> responseBuilder =
			new AtomicReference<>(CreateServiceInstanceResponse.builder());

		return Flux.fromIterable(createServiceInstanceWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.flatMap(workflow -> workflow.buildResponse(request, responseBuilder.get())
				.doOnNext(responseBuilder::set))
			.last(responseBuilder.get())
			.map(CreateServiceInstanceResponseBuilder::build);
	}

	private Mono<Void> create(CreateServiceInstanceRequest request, CreateServiceInstanceResponse response) {
		return stateRepository.saveState(request.getServiceInstanceId(),
			OperationState.IN_PROGRESS,
			"create service instance started")
			.publishOn(Schedulers.parallel())
			.thenMany(invokeCreateWorkflows(request, response)
				.doOnRequest(l -> log.debug("Creating service instance"))
				.doOnComplete(() -> log.debug("Finished creating service instance"))
				.doOnError(exception -> log.error(String.format("Error creating service instance with error '%s'",
						exception.getMessage()), exception)))
			.thenEmpty(stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.SUCCEEDED, "create service instance completed")
				.then())
			.onErrorResume(exception -> stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.FAILED, exception.getMessage())
				.then());
	}

	private Flux<Void> invokeCreateWorkflows(CreateServiceInstanceRequest request,
											 CreateServiceInstanceResponse response) {
		return Flux.fromIterable(createServiceInstanceWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.concatMap(workflow -> workflow.create(request, response));
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return invokeDeleteResponseBuilders(request)
			.doOnNext(response -> delete(request, response)
				.subscribe());
	}

	private Mono<DeleteServiceInstanceResponse> invokeDeleteResponseBuilders(DeleteServiceInstanceRequest request) {
		AtomicReference<DeleteServiceInstanceResponseBuilder> responseBuilder =
			new AtomicReference<>(DeleteServiceInstanceResponse.builder());

		return Flux.fromIterable(deleteServiceInstanceWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.flatMap(workflow -> workflow.buildResponse(request, responseBuilder.get())
				.doOnNext(responseBuilder::set))
			.last(responseBuilder.get())
			.map(DeleteServiceInstanceResponseBuilder::build);
	}

	private Mono<Void> delete(DeleteServiceInstanceRequest request, DeleteServiceInstanceResponse response) {
		return stateRepository.saveState(request.getServiceInstanceId(),
			OperationState.IN_PROGRESS, "delete service instance started")
			.publishOn(Schedulers.parallel())
			.thenMany(invokeDeleteWorkflows(request, response)
				.doOnRequest(l -> log.debug("Deleting service instance"))
				.doOnComplete(() -> log.debug("Finished deleting service instance"))
				.doOnError(exception -> log.error(String.format("Error deleting service instance with error '%s'",
					exception.getMessage()), exception)))
			.thenEmpty(stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.SUCCEEDED, "delete service instance completed")
				.then())
			.onErrorResume(e -> stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.FAILED, e.getMessage())
				.then());
	}

	private Flux<Void> invokeDeleteWorkflows(DeleteServiceInstanceRequest request,
											 DeleteServiceInstanceResponse response) {
		return Flux.fromIterable(deleteServiceInstanceWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.concatMap(workflow -> workflow.delete(request, response));
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		return invokeUpdateResponseBuilders(request)
			.doOnNext(response -> update(request, response)
				.subscribe());
	}

	private Mono<UpdateServiceInstanceResponse> invokeUpdateResponseBuilders(UpdateServiceInstanceRequest request) {
		AtomicReference<UpdateServiceInstanceResponseBuilder> responseBuilder =
			new AtomicReference<>(UpdateServiceInstanceResponse.builder());

		return Flux.fromIterable(updateServiceInstanceWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.flatMap(workflow -> workflow.buildResponse(request, responseBuilder.get())
				.doOnNext(responseBuilder::set))
			.last(responseBuilder.get())
			.map(UpdateServiceInstanceResponseBuilder::build);
	}

	private Mono<Void> update(UpdateServiceInstanceRequest request, UpdateServiceInstanceResponse response) {
		return stateRepository.saveState(request.getServiceInstanceId(),
			OperationState.IN_PROGRESS, "update service instance started")
			.publishOn(Schedulers.parallel())
			.thenMany(invokeUpdateWorkflows(request, response)
				.doOnRequest(l -> log.debug("Updating service instance"))
				.doOnComplete(() -> log.debug("Finished updating service instance"))
				.doOnError(exception -> log.error(String.format("Error updating service instance with error '%s'",
					exception.getMessage()), exception)))
			.thenEmpty(stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.SUCCEEDED, "update service instance completed")
				.then())
			.onErrorResume(exception -> stateRepository.saveState(request.getServiceInstanceId(),
				OperationState.FAILED, exception.getMessage())
				.then());
	}

	private Flux<Void> invokeUpdateWorkflows(UpdateServiceInstanceRequest request,
											 UpdateServiceInstanceResponse response) {
		return Flux.fromIterable(updateServiceInstanceWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.concatMap(workflow -> workflow.update(request, response));
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
}
