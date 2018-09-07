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

import org.springframework.cloud.appbroker.service.UpdateServiceInstanceWorkflow;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformationService;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

public class AppDeploymentUpdateServiceInstanceWorkflow
	extends AppDeploymentInstanceWorkflow
	implements UpdateServiceInstanceWorkflow {
	private final Logger log = Loggers.getLogger(AppDeploymentUpdateServiceInstanceWorkflow.class);

	private BackingAppDeploymentService deploymentService;
	private ParametersTransformationService parametersTransformationService;

	public AppDeploymentUpdateServiceInstanceWorkflow(BrokeredServices brokeredServices,
													  BackingAppDeploymentService deploymentService,
													  ParametersTransformationService parametersTransformationService) {
		super(brokeredServices);
		this.deploymentService = deploymentService;
		this.parametersTransformationService = parametersTransformationService;
	}

	public Mono<String> update(UpdateServiceInstanceRequest request) {
		return getBackingApplicationsForService(request.getServiceDefinition(), request.getPlanId())
			.flatMap(backingApps -> parametersTransformationService.transformParameters(backingApps, request.getParameters()))
			.flatMap(deploymentService::deploy)
			.doOnRequest(l -> log.info("Updating applications {}", brokeredServices))
			.doOnSuccess(d -> log.info("Finished updating applications {}", brokeredServices))
			.doOnError(e -> log.info("Error updating applications {} with error {}", brokeredServices, e));
	}
}
