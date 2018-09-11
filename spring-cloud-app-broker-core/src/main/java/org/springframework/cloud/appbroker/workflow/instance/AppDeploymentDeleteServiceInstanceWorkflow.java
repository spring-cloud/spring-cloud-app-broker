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

import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;

@Order(0)
public class AppDeploymentDeleteServiceInstanceWorkflow
	extends AppDeploymentInstanceWorkflow
	implements DeleteServiceInstanceWorkflow {
	private final BackingAppDeploymentService deploymentService;

	public AppDeploymentDeleteServiceInstanceWorkflow(BrokeredServices brokeredServices,
													  BackingAppDeploymentService deploymentService) {
		super(brokeredServices);
		this.deploymentService = deploymentService;
	}

	@Override
	public Mono<String> delete(DeleteServiceInstanceRequest request) {
		return getBackingApplicationsForService(request.getServiceDefinition(), request.getPlanId())
			.flatMap(deploymentService::undeploy);
	}
}
