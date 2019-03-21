/*
 * Copyright 2016-2019 the original author or authors
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

package org.springframework.cloud.appbroker.acceptance;

import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.manager.BackingAppManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManagementController {

	private final BackingAppManagementService service;

	public ManagementController(BackingAppManagementService service) {
		this.service = service;
	}

	@GetMapping("/start/{serviceInstanceId}")
	public Mono<String> startApplications(@PathVariable String serviceInstanceId) {
		return service.start(serviceInstanceId)
			.thenReturn("starting " + serviceInstanceId);
	}

	@GetMapping("/stop/{serviceInstanceId}")
	public Mono<String> stopApplications(@PathVariable String serviceInstanceId) {
		return service.stop(serviceInstanceId)
			.thenReturn("stopping " + serviceInstanceId);
	}

	@GetMapping("/restart/{serviceInstanceId}")
	public Mono<String> restartApplications(@PathVariable String serviceInstanceId) {
		return service.restart(serviceInstanceId)
			.thenReturn("restarting " + serviceInstanceId);
	}

	@GetMapping("/restage/{serviceInstanceId}")
	public Mono<String> restageApplications(@PathVariable String serviceInstanceId) {
		return service.restage(serviceInstanceId)
			.thenReturn("restaging " + serviceInstanceId);
	}
}
