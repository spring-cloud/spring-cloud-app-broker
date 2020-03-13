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

package org.springframework.cloud.appbroker.integration.fixtures;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServiceKey;
import org.springframework.cloud.appbroker.deployer.BackingServiceKeys;
import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@SuppressWarnings("WeakerAccess")
@TestComponent
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name="service-bindings-as-service-keys")
public class ServiceKeyCreateServiceBindingWorkflow
	extends AbstractServiceInstanceWorkflow
	implements CreateServiceInstanceAppBindingWorkflow {

	private final Logger log = Loggers.getLogger(ServiceKeyCreateServiceBindingWorkflow.class);

	private final BackingServicesProvisionService backingServicesProvisionService;

	private final BackingServicesParametersTransformationService servicesParametersTransformationService;

	private final TargetService targetService;

	public ServiceKeyCreateServiceBindingWorkflow(BrokeredServices brokeredServices,
		BackingServicesProvisionService backingServicesProvisionService,
		BackingServicesParametersTransformationService servicesParametersTransformationService,
		TargetService targetService) {
		super(brokeredServices);
		this.backingServicesProvisionService = backingServicesProvisionService;
		this.servicesParametersTransformationService = servicesParametersTransformationService;
		this.targetService = targetService;
	}

	@Override
	public Mono<Boolean> accept(CreateServiceInstanceBindingRequest request) {
		//Only accept binding request matching one registered backing service
		return accept(request.getServiceDefinition(), request.getPlan());
	}

	@Override
	public Mono<CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder> buildResponse(CreateServiceInstanceBindingRequest request, CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder responseBuilder) {

		return createBackingServiceKey(request).   //create service key, returning credentials
			last(). //only keep last one (should never happen)
			map(responseBuilder::credentials); //store it in builder
	}

	private Flux<Map<String, Object>> createBackingServiceKey(CreateServiceInstanceBindingRequest request) {
		return getBackingServicesForService(request.getServiceDefinition(), request.getPlan())
			.flatMap(backingServices ->
				targetService.addToBackingServices(backingServices,
					getTargetForService(request.getServiceDefinition(), request.getPlan()) ,
					request.getServiceInstanceId()))
			.flatMap(backingServices ->
				servicesParametersTransformationService.transformParameters(backingServices,
					request.getParameters()))
			.flatMap(ServiceKeyCreateServiceBindingWorkflow::getBackingServiceKeys)
			.flatMap(backingServiceKeys -> setServiceKeyName(backingServiceKeys, request.getBindingId()))
			.flatMapMany(backingServicesProvisionService::createServiceKeys)
			.doOnRequest(l -> log.debug("Creating backing service keys for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnComplete(() -> log.debug("Finished creating backing service keys for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnError(exception -> log.error("Error creating backing services keysfor {}/{} with error '{}'",
				request.getServiceDefinition().getName(), request.getPlan().getName(), exceptionMessageOrToString(exception)));
	}

	private static Mono<? extends BackingServiceKeys> getBackingServiceKeys(List<BackingService> backingServices) {
		return Flux.fromIterable(backingServices)
			.flatMap(backingService -> {
				BackingServiceKey backingServiceKey = BackingServiceKey.builder()
					.serviceInstanceName(backingService.getServiceInstanceName())
					.name(backingService.getName())
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
