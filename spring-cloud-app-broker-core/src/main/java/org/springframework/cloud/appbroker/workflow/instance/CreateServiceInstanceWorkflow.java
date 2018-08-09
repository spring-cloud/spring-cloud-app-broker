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

import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import reactor.util.Logger;
import reactor.util.Loggers;

public class CreateServiceInstanceWorkflow {
	private final Logger log = Loggers.getLogger(CreateServiceInstanceWorkflow.class);

	private BackingApplications backingApps;
	private BackingAppDeploymentService deploymentService;

	public CreateServiceInstanceWorkflow(BackingApplications backingApps,
										 BackingAppDeploymentService deploymentService) {
		this.backingApps = backingApps;
		this.deploymentService = deploymentService;
	}

	public Mono<String> create() {
		return deploymentService.deploy(backingApps)
			.doOnRequest(l -> log.info("Deploying applications {}", backingApps))
			.doOnSuccess(d -> log.info("Finished deploying applications {}", backingApps))
			.doOnError(e -> log.info("Error deploying applications {} with error {}", backingApps, e));
	}
}
