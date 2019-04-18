/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.cloud.appbroker.extensions.targets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.appbroker.extensions.ExtensionLocator;

public class TargetService {

	private final ExtensionLocator<Target> locator;

	public TargetService(List<TargetFactory<?>> factories) {
		locator = new ExtensionLocator<>(factories);
	}

	public Mono<List<BackingApplication>> addToBackingApplications(List<BackingApplication> backingApplications,
																   TargetSpec targetSpec,
																   String serviceInstanceId) {
		return Flux.fromIterable(backingApplications)
			.flatMap(backingApplication -> {
				if (targetSpec != null) {
					ArtifactDetails appDetails = getArtifactDetails(targetSpec, serviceInstanceId,
						backingApplication.getName(), backingApplication.getProperties());
					backingApplication.setName(appDetails.getName());
					backingApplication.setProperties(appDetails.getProperties());

					backingApplication.getServices().forEach(servicesSpec -> {
						ArtifactDetails serviceDetails = getArtifactDetails(targetSpec, serviceInstanceId,
							servicesSpec.getServiceInstanceName(), new HashMap<>());
						servicesSpec.setServiceInstanceName(serviceDetails.getName());
					});
				}
				return Mono.just(backingApplication);
			})
			.collectList();
	}

	public Mono<List<BackingService>> addToBackingServices(List<BackingService> backingServices,
														   TargetSpec targetSpec,
														   String serviceInstanceId) {
		return Flux.fromIterable(backingServices)
			.flatMap(backingService -> {
				if (targetSpec != null) {
					ArtifactDetails serviceDetails = getArtifactDetails(targetSpec, serviceInstanceId,
						backingService.getServiceInstanceName(), backingService.getProperties());
					backingService.setServiceInstanceName(serviceDetails.getName());
					backingService.setProperties(serviceDetails.getProperties());
				}
				return Mono.just(backingService);
			})
			.collectList();
	}

	private ArtifactDetails getArtifactDetails(TargetSpec targetSpec, String serviceInstanceId,
											   String name, Map<String, String> properties) {
		Target target = locator.getByName(targetSpec.getName());
		return target.apply(properties, name, serviceInstanceId);
	}
}
