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

package org.springframework.cloud.appbroker.workflow.instance;

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
		return
			getBackingServicesForService(request.getServiceDefinition(), request.getPlanId())
				.flatMap(backingService ->
					targetService.addToBackingServices(backingService,
						getTargetForService(request.getServiceDefinition(), request.getPlanId()),
						request.getServiceInstanceId()))
				.flatMap(backingServices ->
					servicesParametersTransformationService.transformParameters(backingServices,
						request.getParameters()))
				.flatMapMany(backingServicesProvisionService::updateServiceInstance)
				.thenMany(
					getBackingApplicationsForService(request.getServiceDefinition(), request.getPlanId())
						.flatMap(backingApps ->
							targetService.addToBackingApplications(backingApps,
								getTargetForService(request.getServiceDefinition(), request.getPlanId()),
								request.getServiceInstanceId()))
						.flatMap(backingApps ->
							appsParametersTransformationService.transformParameters(backingApps, request.getParameters()))
						.flatMapMany(deploymentService::deploy)
						.doOnRequest(l -> log.debug("Deploying applications {}", brokeredServices))
						.doOnEach(result -> log.debug("Finished deploying {}", result))
						.doOnComplete(() -> log.debug("Finished deploying applications {}", brokeredServices))
						.doOnError(e -> log.error("Error deploying applications {} with error {}", brokeredServices, e)))
				.then();
	}

	@Override
	public Mono<Boolean> accept(UpdateServiceInstanceRequest request) {
		return accept(request.getServiceDefinition(), request.getPlanId());
	}

	@Override
	public Mono<UpdateServiceInstanceResponseBuilder> buildResponse(UpdateServiceInstanceRequest request,
																	UpdateServiceInstanceResponseBuilder responseBuilder) {
		return Mono.just(responseBuilder.async(true));
	}
}
