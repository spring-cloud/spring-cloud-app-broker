/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceRouteBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceRouteBindingResponse.CreateServiceInstanceRouteBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class WorkflowServiceInstanceBindingService implements ServiceInstanceBindingService {

	private final Logger log = Loggers.getLogger(WorkflowServiceInstanceBindingService.class);

	private final List<CreateServiceInstanceAppBindingWorkflow> createServiceInstanceAppBindingWorkflows = new ArrayList<>();

	private final List<CreateServiceInstanceRouteBindingWorkflow> createServiceInstanceRouteBindingWorkflows = new ArrayList<>();

	private final List<DeleteServiceInstanceBindingWorkflow> deleteServiceInstanceBindingWorkflows = new ArrayList<>();

	public WorkflowServiceInstanceBindingService(List<CreateServiceInstanceAppBindingWorkflow> createServiceInstanceAppBindingWorkflows,
												 List<CreateServiceInstanceRouteBindingWorkflow> createServiceInstanceRouteBindingWorkflows,
												 List<DeleteServiceInstanceBindingWorkflow> deleteServiceInstanceBindingWorkflows) {
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
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
		return invokeCreateResponseBuilders(request)
			.publishOn(Schedulers.parallel())
			.doOnNext(response -> create(request)
				.subscribe());
	}

	private Mono<CreateServiceInstanceBindingResponse> invokeCreateResponseBuilders(CreateServiceInstanceBindingRequest request) {
		return Mono.defer(() -> {
			if (isAppBindingRequest(request)) {
				return Mono.just(new AtomicReference<>(CreateServiceInstanceAppBindingResponse.builder()))
					.flatMap(responseBuilder -> Flux.fromIterable(createServiceInstanceAppBindingWorkflows)
						.filterWhen(workflow -> workflow.accept(request))
						.flatMap(workflow -> workflow.buildResponse(request, responseBuilder.get())
							.doOnNext(responseBuilder::set))
						.last(responseBuilder.get())
						.map(CreateServiceInstanceAppBindingResponseBuilder::build));
			}
			else if (isRouteBindingRequest(request)) {
				return Mono.just(new AtomicReference<>(CreateServiceInstanceRouteBindingResponse.builder()))
					.flatMap(responseBuilder -> Flux.fromIterable(createServiceInstanceRouteBindingWorkflows)
						.filterWhen(workflow -> workflow.accept(request))
						.flatMap(workflow -> workflow.buildResponse(request, responseBuilder.get())
							.doOnNext(responseBuilder::set))
						.last(responseBuilder.get())
						.map(CreateServiceInstanceRouteBindingResponseBuilder::build));
			}
			return Mono.empty();
		});
	}

	private boolean isAppBindingRequest(CreateServiceInstanceBindingRequest request) {
		return request.getBindResource() != null
				&& StringUtils.hasText(request.getBindResource().getAppGuid())
				&& StringUtils.isEmpty(request.getBindResource().getRoute());
	}

	private boolean isRouteBindingRequest(CreateServiceInstanceBindingRequest request) {
		return request.getBindResource() != null
				&& StringUtils.isEmpty(request.getBindResource().getAppGuid())
				&& StringUtils.hasText(request.getBindResource().getRoute());
	}

	private Mono<Void> create(CreateServiceInstanceBindingRequest request) {
		return invokeCreateWorkflows(request)
				.doOnRequest(l -> log.info("Creating service instance binding {}", request))
				.doOnComplete(() -> log.info("Finished creating service instance binding {}", request))
				.doOnError(e -> log.info("Error creating service instance binding {} with error {}", request, e))
			.then();
	}

	private Flux<Void> invokeCreateWorkflows(CreateServiceInstanceBindingRequest request) {
		return Flux.defer(() -> {
			if (isAppBindingRequest(request)) {
				return Flux.fromIterable(createServiceInstanceAppBindingWorkflows)
					.filterWhen(workflow -> workflow.accept(request))
					.concatMap(workflow -> workflow.create(request));
			}
			else if (isRouteBindingRequest(request)) {
				return Flux.fromIterable(createServiceInstanceRouteBindingWorkflows)
					.filterWhen(workflow -> workflow.accept(request))
					.concatMap(workflow -> workflow.create(request));
			}
			return Flux.empty();
		});
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
		return invokeDeleteResponseBuilders(request)
			.publishOn(Schedulers.parallel())
			.doOnNext(response -> delete(request)
				.subscribe());
	}

	private Mono<DeleteServiceInstanceBindingResponse> invokeDeleteResponseBuilders(DeleteServiceInstanceBindingRequest request) {
		AtomicReference<DeleteServiceInstanceBindingResponseBuilder> responseBuilder =
			new AtomicReference<>(DeleteServiceInstanceBindingResponse.builder());

		return Flux.fromIterable(deleteServiceInstanceBindingWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.flatMap(workflow -> workflow.buildResponse(request, responseBuilder.get())
				.doOnNext(responseBuilder::set))
			.last(responseBuilder.get())
			.map(DeleteServiceInstanceBindingResponseBuilder::build);
	}

	private Mono<Void> delete(DeleteServiceInstanceBindingRequest request) {
		return invokeDeleteWorkflows(request)
				.doOnRequest(l -> log.info("Deleting service instance binding {}", request))
				.doOnComplete(() -> log.info("Finished deleting service instance binding {}", request))
				.doOnError(e -> log.info("Error deleting service instance binding {} with error {}", request, e))
				.then();
	}

	private Flux<Void> invokeDeleteWorkflows(DeleteServiceInstanceBindingRequest request) {
		return Flux.fromIterable(deleteServiceInstanceBindingWorkflows)
			.filterWhen(workflow -> workflow.accept(request))
			.concatMap(workflow -> workflow.delete(request));
	}

}
