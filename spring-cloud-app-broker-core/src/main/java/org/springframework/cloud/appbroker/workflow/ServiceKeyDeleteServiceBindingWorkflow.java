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

package org.springframework.cloud.appbroker.workflow;

import java.util.List;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServiceKey;
import org.springframework.cloud.appbroker.deployer.BackingServiceKeys;
import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.core.annotation.Order;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

@SuppressWarnings("WeakerAccess")
@Order(0)
public class ServiceKeyDeleteServiceBindingWorkflow
	extends AbstractServiceInstanceWorkflow
	implements DeleteServiceInstanceBindingWorkflow {

	private final Logger log = Loggers.getLogger(ServiceKeyDeleteServiceBindingWorkflow.class);

	private final BackingServicesProvisionService backingServicesProvisionService;

	private final TargetService targetService;

	public ServiceKeyDeleteServiceBindingWorkflow(BrokeredServices brokeredServices,
		BackingServicesProvisionService backingServicesProvisionService,
		TargetService targetService) {
		super(brokeredServices);
		this.backingServicesProvisionService = backingServicesProvisionService;
		this.targetService = targetService;
	}

	@Override
	public Mono<Boolean> accept(DeleteServiceInstanceBindingRequest request) {
		//Only accept binding request matching one registered backing service
		return accept(request.getServiceDefinition(), request.getPlan());
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder> buildResponse(DeleteServiceInstanceBindingRequest request, DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder responseBuilder) {
		return deleteBackingServiceKey(request).
			then(Mono.just(responseBuilder));
	}

	private Flux<String> deleteBackingServiceKey(DeleteServiceInstanceBindingRequest request) {
		return getBackingServicesForService(request.getServiceDefinition(), request.getPlan())
			.flatMap(backingServices ->
				targetService.addToBackingServices(backingServices,
					getTargetForService(request.getServiceDefinition(), request.getPlan()) ,
					request.getServiceInstanceId()))
			.flatMap(ServiceKeyDeleteServiceBindingWorkflow::getBackingServiceKeys)
			.flatMap(backingServiceKeys -> setServiceKeyName(backingServiceKeys, request.getBindingId()))
			.flatMapMany(backingServicesProvisionService::deleteServiceKeys)
			.doOnRequest(l -> log.debug("Deleting backing service keys for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnComplete(() -> log.debug("Finished deleting backing service keys for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnError(exception -> log.error("Error deleting backing services keysfor {}/{} with error '{}'",
				request.getServiceDefinition().getName(), request.getPlan().getName(), exceptionMessageOrToString(exception)));
	}

	private static Mono<? extends BackingServiceKeys> getBackingServiceKeys(List<BackingService> backingServices) {
		return Flux.fromIterable(backingServices)
			.flatMap(backingService -> {
				BackingServiceKey backingServiceKey = BackingServiceKey.builder()
					.serviceInstanceName(backingService.getServiceInstanceName())
					.serviceKeyName(backingService.getName())
					.properties(backingService.getProperties())
					.parameters(backingService.getParameters())
					.build();
				return Mono.just(backingServiceKey);
			})
			.collectList()
			.flatMap(backingServiceKeys -> Mono.just(new BackingServiceKeys(backingServiceKeys)));
	}




	private String exceptionMessageOrToString(Throwable exception) {
		return exception.getMessage() == null ? exception.toString() : exception.getMessage();
	}


}
