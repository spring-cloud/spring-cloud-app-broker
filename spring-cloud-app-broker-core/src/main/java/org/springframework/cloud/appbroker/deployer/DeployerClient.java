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

package org.springframework.cloud.appbroker.deployer;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class DeployerClient {

	private final Logger log = Loggers.getLogger(DeployerClient.class);

	private final AppDeployer appDeployer;

	public DeployerClient(AppDeployer appDeployer) {
		this.appDeployer = appDeployer;
	}

	Mono<String> deploy(BackingApplication backingApplication) {
		return appDeployer.deploy(DeployApplicationRequest.builder()
			.name(backingApplication.getName())
			.path(backingApplication.getPath())
			.properties(backingApplication.getProperties())
			.environment(backingApplication.getEnvironment())
			.services(backingApplication.getServices())
			.build())
			.doOnRequest(l -> log.info("Deploying application {}", backingApplication.getName()))
			.doOnSuccess(d -> log.info("Finished deploying application {}", backingApplication.getName()))
			.doOnError(e -> log.info("Error deploying application {} with error {}", backingApplication.getName(), e))
			.map(DeployApplicationResponse::getName);
	}

	Mono<String> undeploy(BackingApplication backingApplication) {
		return appDeployer.undeploy(UndeployApplicationRequest.builder()
			.properties(backingApplication.getProperties())
			.name(backingApplication.getName())
			.build())
			.map(UndeployApplicationResponse::getName);
	}

}
