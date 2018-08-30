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

import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

public class BackingAppDeploymentService {
	private final Logger log = Loggers.getLogger(BackingAppDeploymentService.class);

	private final DeployerClient deployerClient;

	public BackingAppDeploymentService(DeployerClient deployerClient) {
		this.deployerClient = deployerClient;
	}

	public Mono<String> deploy(List<BackingApplication> backingApps) {
		return Flux.fromIterable(backingApps)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::deploy)
			.doOnRequest(l -> log.info("Deploying applications {}", backingApps))
			.doOnEach(d -> log.info("Finished deploying applications {}", backingApps))
			.doOnError(e -> log.info("Error deploying applications {} with error {}", backingApps, e))
			.sequential()
			.collect(Collectors.joining(","));
	}

	public Mono<String> undeploy(List<BackingApplication> backingApps) {
		return Flux.fromIterable(backingApps)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::undeploy)
			.doOnRequest(l -> log.info("Undeploying applications {}", backingApps))
			.doOnEach(d -> log.info("Finished undeploying applications {}", backingApps))
			.doOnError(e -> log.info("Error undeploying applications {} with error {}", backingApps, e))
			.sequential()
			.collect(Collectors.joining(","));
	}
}
