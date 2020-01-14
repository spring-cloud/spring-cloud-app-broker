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

package org.springframework.cloud.appbroker.manager;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.deployer.BackingApplication;

public class ManagementClient {

	private static final Logger LOG = Loggers.getLogger(ManagementClient.class);

	private final AppManager appManager;

	public ManagementClient(AppManager appManager) {
		this.appManager = appManager;
	}

	public Mono<Void> start(BackingApplication backingApplication) {
		return Mono.justOrEmpty(backingApplication)
			.flatMap(backingApp -> appManager.start(StartApplicationRequest.builder()
				.name(backingApp.getName())
				.properties(backingApp.getProperties())
				.build())
				.doOnRequest(l -> LOG.debug("Starting application {}", backingApp))
				.doOnSuccess(response -> LOG.debug("Finished starting application {}", backingApp))
				.doOnError(exception -> LOG.error(String.format("Error starting application %s with error '%s'",
					backingApp, exception.getMessage()), exception)));
	}

	public Mono<Void> stop(BackingApplication backingApplication) {
		return Mono.justOrEmpty(backingApplication)
			.flatMap(backingApp -> appManager.stop(StopApplicationRequest.builder()
				.name(backingApp.getName())
				.properties(backingApp.getProperties())
				.build())
				.doOnRequest(l -> LOG.debug("Stopping application {}", backingApp))
				.doOnSuccess(response -> LOG.debug("Finished stopping application {}", backingApp))
				.doOnError(exception -> LOG.error(String.format("Error stopping application %s with error '%s'",
					backingApp, exception.getMessage()), exception)));
	}

	public Mono<Void> restart(BackingApplication backingApplication) {
		return Mono.justOrEmpty(backingApplication)
			.flatMap(backingApp -> appManager.restart(RestartApplicationRequest.builder()
				.name(backingApp.getName())
				.properties(backingApp.getProperties())
				.build())
				.doOnRequest(l -> LOG.debug("Restarting application {}", backingApp))
				.doOnSuccess(response -> LOG.debug("Finished restarting application {}", backingApp))
				.doOnError(exception -> LOG.error(String.format("Error restarting application %s with error '%s'",
					backingApp, exception.getMessage()), exception)));
	}

	public Mono<Void> restage(BackingApplication backingApplication) {
		return Mono.justOrEmpty(backingApplication)
			.flatMap(backingApp -> appManager.restage(RestageApplicationRequest.builder()
				.name(backingApp.getName())
				.properties(backingApp.getProperties())
				.build())
				.doOnRequest(l -> LOG.debug("Restaging application {}", backingApp))
				.doOnSuccess(response -> LOG.debug("Finished restaging application {}", backingApp))
				.doOnError(exception -> LOG.error(String.format("Error restaging application %s with error '%s'",
					backingApp, exception.getMessage()), exception)));
	}

}
