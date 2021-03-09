/*
 * Copyright 2002-2021 the original author or authors.
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

public class DefaultBackingSpaceManagementService implements BackingSpaceManagementService {

	private static final Logger LOG = Loggers.getLogger(DefaultBackingSpaceManagementService.class);

	private static final String BACKINGSPACES_LOG_TEMPLATE = "backingSpaces={}";

	private final DeployerClient deployerClient;

	public DefaultBackingSpaceManagementService(DeployerClient deployerClient) {
		this.deployerClient = deployerClient;
	}

	@Override
	public Flux<String> deleteTargetSpaces(List<String> targetSpaces) {
		return Flux.fromIterable(targetSpaces)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::deleteSpace)
			.sequential()
			.doOnRequest(l -> {
				LOG.info("Deleting backing spaces");
				LOG.debug(BACKINGSPACES_LOG_TEMPLATE, targetSpaces);
			})
			.doOnComplete(() -> {
				LOG.info("Finish deleting backing spaces");
				LOG.debug(BACKINGSPACES_LOG_TEMPLATE, targetSpaces);
			})
			.doOnError(e -> LOG.error(String.format("Error deleting backing spaces. error=%s", e.getMessage()), e));
	}

}
