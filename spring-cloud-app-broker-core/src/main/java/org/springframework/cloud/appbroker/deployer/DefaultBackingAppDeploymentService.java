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

public class DefaultBackingAppDeploymentService implements BackingAppDeploymentService {

	private final Logger log = Loggers.getLogger(DefaultBackingAppDeploymentService.class);

	private final DeployerClient deployerClient;

	public DefaultBackingAppDeploymentService(DeployerClient deployerClient) {
		this.deployerClient = deployerClient;
	}

	@Override
	public Flux<String> deploy(List<BackingApplication> backingApps, String serviceInstanceId) {
		return Flux.fromIterable(backingApps)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(backingApplication -> deployerClient.deploy(backingApplication, serviceInstanceId))
			.sequential()
			.doOnRequest(l -> log.debug("Deploying applications {}", backingApps))
			.doOnEach(response -> log.debug("Finished deploying application {}", response))
			.doOnComplete(() -> log.debug("Finished deploying application {}", backingApps))
			.doOnError(exception -> log.error(String.format("Error deploying applications %s with error '%s'",
				backingApps, exception.getMessage()), exception));
	}

	@Override
	public Flux<String> update(List<BackingApplication> backingApps, String serviceInstanceId) {
		return Flux.fromIterable(backingApps)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(backingApplication -> deployerClient.update(backingApplication, serviceInstanceId))
			.sequential()
			.doOnRequest(l -> log.debug("Updating applications {}", backingApps))
			.doOnEach(response -> log.debug("Finished updating application {}", response))
			.doOnComplete(() -> log.debug("Finished updating application {}", backingApps))
			.doOnError(exception -> log
				.error(String.format("Error updating applications %s with error '%s'", backingApps, exception)));
	}

	@Override
	public Flux<String> undeploy(List<BackingApplication> backingApps) {
		return Flux.fromIterable(backingApps)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::undeploy)
			.sequential()
			.doOnRequest(l -> log.debug("Undeploying applications {}", backingApps))
			.doOnEach(response -> log.debug("Finished undeploying application {}", response))
			.doOnComplete(() -> log.debug("Finished undeploying application {}", backingApps))
			.doOnError(exception -> log.error(String.format("Error undeploying applications %s with error '%s'",
				backingApps, exception.getMessage()), exception));
	}

}
