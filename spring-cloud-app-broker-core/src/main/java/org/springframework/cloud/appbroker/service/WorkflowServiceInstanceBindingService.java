/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.state.ServiceInstanceBindingStateRepository;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceRouteBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceRouteBindingResponse.CreateServiceInstanceRouteBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationRequest;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class WorkflowServiceInstanceBindingService implements ServiceInstanceBindingService {

	private static final Logger LOG = Loggers.getLogger(WorkflowServiceInstanceBindingService.class);

	private final ServiceInstanceBindingStateRepository stateRepository;

	private final List<CreateServiceInstanceAppBindingWorkflow> createServiceInstanceAppBindingWorkflows = new ArrayList<>();

	private final List<CreateServiceInstanceRouteBindingWorkflow> createServiceInstanceRouteBindingWorkflows = new ArrayList<>();

	private final List<DeleteServiceInstanceBindingWorkflow> deleteServiceInstanceBindingWorkflows = new ArrayList<>();

	public WorkflowServiceInstanceBindingService(
		ServiceInstanceBindingStateRepository serviceInstanceBindingStateRepository,
		List<CreateServiceInstanceAppBindingWorkflow> createServiceInstanceAppBindingWorkflows,
		List<CreateServiceInstanceRouteBindingWorkflow> createServiceInstanceRouteBindingWorkflows,
		List<DeleteServiceInstanceBindingWorkflow> deleteServiceInstanceBindingWorkflows) {
		this.stateRepository = serviceInstanceBindingStateRepository;
		if (!CollectionUtils.isEmpty(createServiceInstanceAppBindingWorkflows)) {
			this.createServiceInstanceAppBindingWorkflows.addAll(createServiceInstanceAppBindingWorkflows);
		}
		if (!CollectionUtils.isEmpty(createServiceInstanceRouteBindingWorkflows)) {
			this.createServiceInstanceRouteBindingWorkflows.addAll(createServiceInstanceRouteBindingWorkflows);
		}
		if (!CollectionUtils.isEmpty(deleteServiceInstanceBindingWorkflows)) {
			this.deleteServiceInstanceBindingWorkflows.addAll(deleteServiceInstanceBindingWorkflows);
		}
		AnnotationAwareOrderComparator.sort(this.createServiceInstanceAppBindingWorkflows);
		AnnotationAwareOrderComparator.sort(this.createServiceInstanceRouteBindingWorkflows);
		AnnotationAwareOrderComparator.sort(this.deleteServiceInstanceBindingWorkflows);
	}

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
		CreateServiceInstanceBindingRequest request) {
		return invokeCreateResponseBuilders(request)
			.publishOn(Schedulers.parallel())
			.doOnNext(response -> create(request, response)
				.subscribe());
	}

	private Mono<CreateServiceInstanceBindingResponse> invokeCreateResponseBuilders(
		CreateServiceInstanceBindingRequest request) {
		return Mono.defer(() -> {
			if (isAppBindingRequest(request)) {
				CreateServiceInstanceAppBindingResponseBuilder builder = CreateServiceInstanceAppBindingResponse
					.builder();
				return invokeAppBindingBuildResponse(builder, request, this.createServiceInstanceAppBindingWorkflows)
					.last(builder)
					.map(CreateServiceInstanceAppBindingResponseBuilder::build);
			}
			else if (isRouteBindingRequest(request)) {
				CreateServiceInstanceRouteBindingResponseBuilder builder = CreateServiceInstanceRouteBindingResponse
					.builder();
				return invokeRouteBindingBuildResponse(builder, request,
					this.createServiceInstanceRouteBindingWorkflows)
					.last(builder)
					.map(CreateServiceInstanceRouteBindingResponseBuilder::build);
			}
			return Mono.empty();
		});
	}

	private Flux<CreateServiceInstanceAppBindingResponseBuilder> invokeAppBindingBuildResponse(
		CreateServiceInstanceAppBindingResponseBuilder builder, CreateServiceInstanceBindingRequest request,
		List<CreateServiceInstanceAppBindingWorkflow> workflows) {
		AtomicReference<CreateServiceInstanceAppBindingResponseBuilder> responseBuilder = new AtomicReference<>(
			builder);
		return Flux.fromIterable(workflows)
			.filterWhen(workflow -> workflow.accept(request))
			.concatMap(workflow -> workflow.buildResponse(request, responseBuilder.get())
				.doOnNext(responseBuilder::set));
	}

	private Flux<CreateServiceInstanceRouteBindingResponseBuilder> invokeRouteBindingBuildResponse(
		CreateServiceInstanceRouteBindingResponseBuilder builder, CreateServiceInstanceBindingRequest request,
		List<CreateServiceInstanceRouteBindingWorkflow> workflows) {
		AtomicReference<CreateServiceInstanceRouteBindingResponseBuilder> responseBuilder = new AtomicReference<>(
			builder);
		return Flux.fromIterable(workflows)
			.filterWhen(workflow -> workflow.accept(request))
			.concatMap(workflow -> workflow.buildResponse(request, responseBuilder.get())
				.doOnNext(responseBuilder::set));
	}

	private boolean isAppBindingRequest(CreateServiceInstanceBindingRequest request) {
		boolean missingBindResource = request.getBindResource() == null;
			boolean notRouteBindingRequest = request.getBindResource() != null && StringUtils.isEmpty(request.getBindResource().getRoute());
		return missingBindResource || notRouteBindingRequest;
	}

	private boolean isRouteBindingRequest(CreateServiceInstanceBindingRequest request) {
		return request.getBindResource() != null
			&& StringUtils.hasText(request.getBindResource().getRoute());
	}

	private Mono<Void> create(CreateServiceInstanceBindingRequest request,
		CreateServiceInstanceBindingResponse response) {
		return stateRepository.saveState(request.getServiceInstanceId(), request.getBindingId(),
			OperationState.IN_PROGRESS, "create service instance binding started")
			.thenMany(invokeCreateWorkflows(request, response)
				.doOnRequest(l -> LOG.debug("Creating service instance binding"))
				.doOnComplete(() -> LOG.debug("Finished creating service instance binding"))
				.doOnError(exception -> LOG.error(String.format("Error creating service instance binding with error " +
					"'%s'", exception.getMessage()), exception)))
			.thenEmpty(stateRepository.saveState(request.getServiceInstanceId(), request.getBindingId(),
				OperationState.SUCCEEDED, "create service instance binding completed")
				.then())
			.onErrorResume(
				exception -> stateRepository.saveState(request.getServiceInstanceId(), request.getBindingId(),
					OperationState.FAILED, exception.getMessage())
					.then());
	}

	private Flux<Void> invokeCreateWorkflows(CreateServiceInstanceBindingRequest request,
		CreateServiceInstanceBindingResponse response) {
		return Flux.defer(() -> {
			if (isAppBindingRequest(request)) {
				return Flux.fromIterable(createServiceInstanceAppBindingWorkflows)
					.filterWhen(workflow -> workflow.accept(request))
					.concatMap(workflow -> workflow.create(request,
						(CreateServiceInstanceAppBindingResponse) response));
			}
			else if (isRouteBindingRequest(request)) {
				return Flux.fromIterable(createServiceInstanceRouteBindingWorkflows)
					.filterWhen(workflow -> workflow.accept(request))
					.concatMap(workflow -> workflow.create(request,
						(CreateServiceInstanceRouteBindingResponse) response));
			}
			return Flux.empty();
		});
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
		DeleteServiceInstanceBindingRequest request) {
		return invokeDeleteResponseBuilders(request)
			.publishOn(Schedulers.parallel())
			.doOnNext(response -> delete(request, response)
				.subscribe());
	}

	private Mono<DeleteServiceInstanceBindingResponse> invokeDeleteResponseBuilders(
		DeleteServiceInstanceBindingRequest request) {
		AtomicReference<DeleteServiceInstanceBindingResponseBuilder> responseBuilder =
			new AtomicReference<>(DeleteServiceInstanceBindingResponse.builder());

		return Flux.fromIterable(deleteServiceInstanceBindingWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.flatMap(workflow -> workflow.buildResponse(request, responseBuilder.get())
				.doOnNext(responseBuilder::set))
			.last(responseBuilder.get())
			.map(DeleteServiceInstanceBindingResponseBuilder::build);
	}

	private Mono<Void> delete(DeleteServiceInstanceBindingRequest request,
		DeleteServiceInstanceBindingResponse response) {
		return stateRepository.saveState(request.getServiceInstanceId(), request.getBindingId(),
			OperationState.IN_PROGRESS, "delete service instance binding started")
			.thenMany(invokeDeleteWorkflows(request, response)
				.doOnRequest(l -> LOG.debug("Deleting service instance binding"))
				.doOnComplete(() -> LOG.debug("Finished deleting service instance binding"))
				.doOnError(exception -> LOG.error(String.format("Error deleting service instance binding with error " +
					"'%s'", exception.getMessage()), exception)))
			.thenEmpty(stateRepository.saveState(request.getServiceInstanceId(), request.getBindingId(),
				OperationState.SUCCEEDED, "delete service instance binding completed")
				.then())
			.onErrorResume(
				exception -> stateRepository.saveState(request.getServiceInstanceId(), request.getBindingId(),
					OperationState.FAILED, exception.getMessage())
					.then());
	}

	private Flux<Void> invokeDeleteWorkflows(DeleteServiceInstanceBindingRequest request,
		DeleteServiceInstanceBindingResponse response) {
		return Flux.fromIterable(deleteServiceInstanceBindingWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.concatMap(workflow -> workflow.delete(request, response));
	}

	@Override
	public Mono<GetLastServiceBindingOperationResponse> getLastOperation(
		GetLastServiceBindingOperationRequest request) {
		return stateRepository.getState(request.getServiceInstanceId(), request.getBindingId())
			.doOnError(exception -> Mono.error(new ServiceInstanceBindingDoesNotExistException(request.getBindingId())))
			.map(serviceInstanceState -> GetLastServiceBindingOperationResponse.builder()
				.operationState(serviceInstanceState.getOperationState())
				.description(serviceInstanceState.getDescription())
				.build());
	}

}
