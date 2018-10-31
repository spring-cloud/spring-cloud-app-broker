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
				   .doOnRequest(l -> log.info("Creating backing services {}", backingServices))
				   .doOnEach(d -> log.info("Finished creating backing service {}", d))
				   .doOnComplete(() -> log.info("Finished creating backing service {}", backingServices))
				   .doOnError(e -> log.info("Error creating backing services {} with error {}", backingServices, e))
				   .sequential();
	}

	public Flux<String> deleteServiceInstance(List<BackingService> backingServices) {
		return Flux.fromIterable(backingServices)
				   .parallel()
				   .runOn(Schedulers.parallel())
				   .flatMap(deployerClient::deleteServiceInstance)
				   .doOnRequest(l -> log.info("Deleting backing services {}", backingServices))
				   .doOnEach(d -> log.info("Finished deleting backing service {}", d))
				   .doOnComplete(() -> log.info("Finished deleting backing service {}", backingServices))
				   .doOnError(e -> log.info("Error deleting backing services {} with error {}", backingServices, e))
				   .sequential();
	}

}
