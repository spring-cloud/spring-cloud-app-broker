/*
 * Copyright 2002-2020 the original author or authors.
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

public class DefaultBackingServicesProvisionService implements BackingServicesProvisionService {

	private static final Logger LOG = Loggers.getLogger(DefaultBackingServicesProvisionService.class);

	private static final String BACKINGSERVICES_LOG_TEMPLATE = "backingServices={}";

	private final DeployerClient deployerClient;

	public DefaultBackingServicesProvisionService(DeployerClient deployerClient) {
		this.deployerClient = deployerClient;
	}

	@Override
	public Flux<String> createServiceInstance(List<BackingService> backingServices) {
		return Flux.fromIterable(backingServices)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::createServiceInstance)
			.sequential()
			.doOnRequest(l -> {
				LOG.info("Creating backing services");
				LOG.debug(BACKINGSERVICES_LOG_TEMPLATE, backingServices);
			})
			.doOnComplete(() -> {
				LOG.info("Finish creating backing services");
				LOG.debug(BACKINGSERVICES_LOG_TEMPLATE, backingServices);
			})
			.doOnError(e -> LOG.error(String.format("Error creating backing services. error=%s", e.getMessage()), e));
	}

	@Override
	public Flux<String> updateServiceInstance(List<BackingService> backingServices) {
		return Flux.fromIterable(backingServices)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::updateServiceInstance)
			.sequential()
			.doOnRequest(l -> {
				LOG.info("Updating backing services");
				LOG.debug(BACKINGSERVICES_LOG_TEMPLATE, backingServices);
			})
			.doOnComplete(() -> {
				LOG.info("Finish updating backing services");
				LOG.debug(BACKINGSERVICES_LOG_TEMPLATE, backingServices);
			})
			.doOnError(e -> LOG.error(String.format("Error updating backing services. error=%s", e.getMessage()), e));
	}

	@Override
	public Flux<String> deleteServiceInstance(List<BackingService> backingServices) {
		return Flux.fromIterable(backingServices)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::deleteServiceInstance)
			.sequential()
			.doOnRequest(l -> {
				LOG.info("Deleting backing services");
				LOG.debug(BACKINGSERVICES_LOG_TEMPLATE, backingServices);
			})
			.doOnComplete(() -> {
				LOG.info("Finish deleting backing services");
				LOG.debug(BACKINGSERVICES_LOG_TEMPLATE, backingServices);
			})
			.doOnError(e -> LOG.error(String.format("Error deleting backing services. error=%s", e.getMessage()), e));
	}

}
