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

	private static final Logger LOG = Loggers.getLogger(DefaultBackingAppDeploymentService.class);

	private static final String BACKINGAPPS_LOG_TEMPLATE = "backingApps={}";

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
			.doOnRequest(l -> {
				LOG.info("Deploying applications");
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			})
			.doOnComplete(() -> {
				LOG.info("Finish deploying applications");
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error deploying applications. error=%s", e.getMessage()), e);
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			});
	}

	@Override
	public Flux<String> prepareForUpdate(List<BackingApplication> backingApps, String serviceInstanceId) {
		return Flux.fromIterable(backingApps)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(backingApplication -> deployerClient.preUpdate(backingApplication, serviceInstanceId))
			.sequential()
			.doOnRequest(l -> {
				LOG.info("Preparing applications for update");
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			})
			.doOnComplete(() -> {
				LOG.info("Finish preparing applications for update");
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error preparing applications for update. error=%s", e.getMessage()), e);
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			});
	}

	@Override
	public Flux<String> update(List<BackingApplication> backingApps, String serviceInstanceId) {
		return Flux.fromIterable(backingApps)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(backingApplication -> deployerClient.update(backingApplication, serviceInstanceId))
			.sequential()
			.doOnRequest(l -> {
				LOG.info("Updating applications");
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			})
			.doOnComplete(() -> {
				LOG.info("Finish updating applications");
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error updating applications. error=%s", e.getMessage()), e);
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			});
	}

	@Override
	public Flux<String> undeploy(List<BackingApplication> backingApps) {
		return Flux.fromIterable(backingApps)
			.parallel()
			.runOn(Schedulers.parallel())
			.flatMap(deployerClient::undeploy)
			.sequential()
			.doOnRequest(l -> {
				LOG.info("Undeploying applications");
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			})
			.doOnComplete(() -> {
				LOG.info("Finish undeploying applications");
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error undeploying applications. error=%s", e.getMessage()), e);
				LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
			});
	}

}
