/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.deployer;

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

public class BackingServicesProvisionService {

	private final Logger log = Loggers.getLogger(BackingServicesProvisionService.class);

	private final DeployerClient deployerClient;

	public BackingServicesProvisionService(DeployerClient deployerClient) {
		this.deployerClient = deployerClient;
	}

	public Flux<String> createServiceInstance(List<BackingService> backingServices) {
		return Flux.fromIterable(backingServices)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::createServiceInstance)
			.sequential()
			.doOnRequest(l -> log.debug("Creating backing services {}", backingServices))
			.doOnEach(response -> log.debug("Finished creating backing service {}", response))
			.doOnComplete(() -> log.debug("Finished creating backing services {}", backingServices))
			.doOnError(exception -> log.error("Error creating backing services {} with error '{}'",
				backingServices, exception.getMessage()));
	}

	public Flux<String> updateServiceInstance(List<BackingService> backingServices) {
		return Flux.fromIterable(backingServices)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::updateServiceInstance)
			.sequential()
			.doOnRequest(l -> log.debug("Updating backing services {}", backingServices))
			.doOnEach(response -> log.debug("Finished updating backing service {}", response))
			.doOnComplete(() -> log.debug("Finished updating backing services {}", backingServices))
			.doOnError(exception -> log.error("Error updating backing services {} with error '{}'",
				backingServices, exception.getMessage()));
	}

	public Flux<String> deleteServiceInstance(List<BackingService> backingServices) {
		return Flux.fromIterable(backingServices)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::deleteServiceInstance)
			.sequential()
			.doOnRequest(l -> log.debug("Deleting backing services {}", backingServices))
			.doOnEach(response -> log.debug("Finished deleting backing service {}", response))
			.doOnComplete(() -> log.debug("Finished deleting backing services {}", backingServices))
			.doOnError(exception -> log.error("Error deleting backing services {} with error '{}'",
				backingServices, exception.getMessage()));
	}
}
