/*
 * Copyright 2002-2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.manager.AppManager;
import org.springframework.cloud.appbroker.manager.RestageApplicationRequest;
import org.springframework.cloud.appbroker.manager.RestartApplicationRequest;
import org.springframework.cloud.appbroker.manager.StartApplicationRequest;
import org.springframework.cloud.appbroker.manager.StopApplicationRequest;

public class CloudFoundryAppManager implements AppManager {

	private static final Logger LOG = LoggerFactory.getLogger(CloudFoundryAppManager.class);

	private final CloudFoundryOperationsUtils operationsUtils;

	public CloudFoundryAppManager(CloudFoundryOperationsUtils operationsUtils) {
		this.operationsUtils = operationsUtils;
	}

	@Override
	public Mono<Void> start(StartApplicationRequest request) {
		return Mono.justOrEmpty(request)
			.flatMap(req -> operationsUtils.getOperations(req.getProperties())
				.flatMap(cfOperations -> Mono.justOrEmpty(req.getName())
					.flatMap(appName -> cfOperations.applications().start(
						org.cloudfoundry.operations.applications.StartApplicationRequest.builder()
							.name(appName)
							.build())
						.doOnRequest(l -> LOG.debug("Starting application {}", appName))
						.doOnSuccess(item -> LOG.info("Successfully started application {}", appName))
						.doOnError(error -> LOG.error("Failed to start application {}", appName)))));
	}

	@Override
	public Mono<Void> stop(StopApplicationRequest request) {
		return Mono.justOrEmpty(request)
			.flatMap(req -> operationsUtils.getOperations(req.getProperties())
				.flatMap(cfOperations -> Mono.justOrEmpty(req.getName())
					.flatMap(appName -> cfOperations.applications().stop(
						org.cloudfoundry.operations.applications.StopApplicationRequest.builder()
							.name(appName)
							.build())
						.doOnRequest(l -> LOG.debug("Stopping application {}", appName))
						.doOnSuccess(item -> LOG.info("Successfully stopped application {}", appName))
						.doOnError(error -> LOG.error("Failed to stop application {}", appName)))));
	}

	@Override
	public Mono<Void> restart(RestartApplicationRequest request) {
		return Mono.justOrEmpty(request)
			.flatMap(req -> operationsUtils.getOperations(req.getProperties())
				.flatMap(cfOperations -> Mono.justOrEmpty(req.getName())
					.flatMap(appName -> cfOperations.applications().restart(
						org.cloudfoundry.operations.applications.RestartApplicationRequest.builder()
							.name(appName)
							.build())
						.doOnRequest(l -> LOG.debug("Restarting application {}", appName))
						.doOnSuccess(item -> LOG.info("Successfully restarted application {}", appName))
						.doOnError(error -> LOG.error("Failed to restart application {}", appName)))));
	}

	@Override
	public Mono<Void> restage(RestageApplicationRequest request) {
		return Mono.justOrEmpty(request)
			.flatMap(req -> operationsUtils.getOperations(req.getProperties())
				.flatMap(cfOperations -> Mono.justOrEmpty(req.getName())
					.flatMap(appName -> cfOperations.applications().restage(
						org.cloudfoundry.operations.applications.RestageApplicationRequest.builder()
							.name(appName)
							.build())
						.doOnRequest(l -> LOG.debug("Restaging application {}", appName))
						.doOnSuccess(item -> LOG.info("Successfully restaged application {}", appName))
						.doOnError(error -> LOG.error("Failed to restage application {}", appName)))));
	}

}
