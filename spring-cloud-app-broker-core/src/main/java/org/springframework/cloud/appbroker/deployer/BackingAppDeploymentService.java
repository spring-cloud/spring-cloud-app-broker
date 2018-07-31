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

import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BackingAppDeploymentService {

	private final DeployerClient deployerClient;

	public BackingAppDeploymentService(DeployerClient deployerClient) {
		this.deployerClient = deployerClient;
	}

	public Mono<String> deploy(BackingApplications backingApps) {
		return Flux.fromIterable(backingApps)
			.flatMap(deployerClient::deploy)
			.collect(Collectors.joining(","));
	}

	public Mono<String> undeploy(BackingApplications backingApps) {
		return Flux.fromIterable(backingApps)
			.flatMap(deployerClient::undeploy)
			.collect(Collectors.joining(","));
	}
}
