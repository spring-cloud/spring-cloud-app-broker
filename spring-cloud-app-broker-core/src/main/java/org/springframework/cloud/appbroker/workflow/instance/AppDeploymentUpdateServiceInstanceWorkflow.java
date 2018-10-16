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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.UpdateServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse.UpdateServiceInstanceResponseBuilder;
import org.springframework.core.annotation.Order;

@Order(0)
public class AppDeploymentUpdateServiceInstanceWorkflow
	extends AppDeploymentInstanceWorkflow
	implements UpdateServiceInstanceWorkflow {

	private final Logger log = Loggers.getLogger(AppDeploymentUpdateServiceInstanceWorkflow.class);

	private final BackingAppDeploymentService deploymentService;
	private final ParametersTransformationService parametersTransformationService;
	private final TargetService targetService;

	public AppDeploymentUpdateServiceInstanceWorkflow(BrokeredServices brokeredServices,
													  BackingAppDeploymentService deploymentService,
													  ParametersTransformationService parametersTransformationService,
													  TargetService targetService) {
		super(brokeredServices);
		this.deploymentService = deploymentService;
		this.parametersTransformationService = parametersTransformationService;
		this.targetService = targetService;
	}

	public Flux<Void> update(UpdateServiceInstanceRequest request) {
		return getBackingApplicationsForService(request.getServiceDefinition(), request.getPlanId())
			.flatMap(backingApps -> targetService.add(backingApps, request.getServiceInstanceId()))
			.flatMap(backingApps ->
				parametersTransformationService.transformParameters(backingApps, request.getParameters()))
			.flatMapMany(deploymentService::deploy)
			.doOnRequest(l -> log.info("Deploying applications {}", brokeredServices))
			.doOnEach(s -> log.info("Finished deploying {}", s))
			.doOnComplete(() -> log.info("Finished deploying applications {}", brokeredServices))
			.doOnError(e -> log.info("Error deploying applications {} with error {}", brokeredServices, e))
			.flatMap(apps -> Flux.empty());
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
