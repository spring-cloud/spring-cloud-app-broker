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
			.services(backingApplication.getServices().stream().map(ServicesSpec::getServiceInstanceName).collect(Collectors.toList()))
			.build())
			.doOnRequest(l -> log.debug("Deploying application {}", backingApplication))
			.doOnSuccess(response -> log.debug("Finished deploying application {}", backingApplication))
			.doOnError(exception -> log.error("Error deploying application {} with error {}", backingApplication, exception))
			.map(DeployApplicationResponse::getName);
	}

	Mono<String> undeploy(BackingApplication backingApplication) {
		return appDeployer.undeploy(UndeployApplicationRequest.builder()
			.properties(backingApplication.getProperties())
			.name(backingApplication.getName())
			.build())
			.map(UndeployApplicationResponse::getName);
	}

	Mono<String> createServiceInstance(BackingService backingService) {
		return appDeployer.createServiceInstance(
			CreateServiceInstanceRequest.builder()
				.serviceInstanceName(backingService.getServiceInstanceName())
				.name(backingService.getName())
				.plan(backingService.getPlan())
				.parameters(backingService.getParameters())
				.properties(backingService.getProperties())
				.build())
			.doOnRequest(l -> log.debug("Creating backing service {}", backingService.getName()))
			.doOnSuccess(d -> log.debug("Finished creating backing service {}", backingService.getName()))
			.doOnError(e -> log.error("Error creating backing service {} with error {}", backingService.getName(), e))
			.map(CreateServiceInstanceResponse::getName);
	}

	Mono<String> updateServiceInstance(BackingService backingService) {
		return appDeployer.updateServiceInstance(
			UpdateServiceInstanceRequest.builder()
				.serviceInstanceName(backingService.getServiceInstanceName())
				.parameters(backingService.getParameters())
				.properties(backingService.getProperties())
				.rebindOnUpdate(backingService.isRebindOnUpdate())
				.build())
			.doOnRequest(l -> log.debug("Creating backing service {}", backingService.getName()))
			.doOnSuccess(d -> log.debug("Finished creating backing service {}", backingService.getName()))
			.doOnError(e -> log.error("Error creating backing service {} with error {}", backingService.getName(), e))
			.map(UpdateServiceInstanceResponse::getName);
	}

	Mono<String> deleteServiceInstance(BackingService backingService) {
		return appDeployer.deleteServiceInstance(
			DeleteServiceInstanceRequest.builder()
				.serviceInstanceName(backingService.getServiceInstanceName())
				.properties(backingService.getProperties())
				.build())
			.map(DeleteServiceInstanceResponse::getName);
	}
}
