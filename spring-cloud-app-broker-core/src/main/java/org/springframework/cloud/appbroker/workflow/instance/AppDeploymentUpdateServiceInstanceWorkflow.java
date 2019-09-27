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

package org.springframework.cloud.appbroker.workflow.instance;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.UpdateServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse.UpdateServiceInstanceResponseBuilder;
import org.springframework.core.annotation.Order;

@Order(0)
public class AppDeploymentUpdateServiceInstanceWorkflow
	extends AppDeploymentInstanceWorkflow
	implements UpdateServiceInstanceWorkflow {

	private final Logger log = Loggers.getLogger(AppDeploymentUpdateServiceInstanceWorkflow.class);

	private final BackingAppDeploymentService deploymentService;
	private final BackingServicesProvisionService backingServicesProvisionService;
	private final BackingApplicationsParametersTransformationService appsParametersTransformationService;
	private final BackingServicesParametersTransformationService servicesParametersTransformationService;
	private final TargetService targetService;

	public AppDeploymentUpdateServiceInstanceWorkflow(BrokeredServices brokeredServices,
													  BackingAppDeploymentService deploymentService,
													  BackingServicesProvisionService backingServicesProvisionService,
													  BackingApplicationsParametersTransformationService appsParametersTransformationService,
													  BackingServicesParametersTransformationService servicesParametersTransformationService,
													  TargetService targetService) {
		super(brokeredServices);
		this.deploymentService = deploymentService;
		this.backingServicesProvisionService = backingServicesProvisionService;
		this.appsParametersTransformationService = appsParametersTransformationService;
		this.servicesParametersTransformationService = servicesParametersTransformationService;
		this.targetService = targetService;
	}

	public Mono<Void> update(UpdateServiceInstanceRequest request, UpdateServiceInstanceResponse response) {
		return updateBackingServices(request)
			.thenMany(updateBackingApplications(request))
			.then();
	}

	private Flux<String> updateBackingServices(UpdateServiceInstanceRequest request) {
		return getBackingServicesForService(request.getServiceDefinition(), request.getPlan())
			.flatMap(backingServices ->
				targetService.addToBackingServices(backingServices,
					getTargetForService(request.getServiceDefinition(), request.getPlan()),
					request.getServiceInstanceId()))
			.flatMap(backingServices ->
				servicesParametersTransformationService.transformParameters(backingServices,
					request.getParameters()))
			.flatMapMany(backingServicesProvisionService::updateServiceInstance)
			.doOnRequest(l -> log.debug("Updating backing services for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnComplete(() -> log.debug("Finished updating backing services for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnError(exception -> log.error(String.format("Error updating backing services for %s/%s with error '%s'",
				request.getServiceDefinition().getName(), request.getPlan().getName(), exception.getMessage()), exception));
	}

	private Flux<String> updateBackingApplications(UpdateServiceInstanceRequest request) {
		return getBackingApplicationsForService(request.getServiceDefinition(), request.getPlan())
			.flatMap(backingApps ->
				targetService.addToBackingApplications(backingApps,
					getTargetForService(request.getServiceDefinition(), request.getPlan()),
					request.getServiceInstanceId()))
			.flatMap(backingApps ->
				appsParametersTransformationService.transformParameters(backingApps, request.getParameters()))
			.flatMapMany(backingApps -> deploymentService.update(backingApps, request.getServiceInstanceId()))
			.doOnRequest(l -> log.debug("Updating backing applications for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnComplete(() -> log.debug("Finished updating backing applications for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnError(exception -> log.error(String.format("Error updating backing applications for %s/%s with error '%s'",
				request.getServiceDefinition().getName(), request.getPlan().getName(), exception.getMessage()), exception));
	}

	@Override
	public Mono<Boolean> accept(UpdateServiceInstanceRequest request) {
		return accept(request.getServiceDefinition(), request.getPlan());
	}

	@Override
	public Mono<UpdateServiceInstanceResponseBuilder> buildResponse(UpdateServiceInstanceRequest request,
																	UpdateServiceInstanceResponseBuilder responseBuilder) {
		return Mono.just(responseBuilder.async(true));
	}
}
