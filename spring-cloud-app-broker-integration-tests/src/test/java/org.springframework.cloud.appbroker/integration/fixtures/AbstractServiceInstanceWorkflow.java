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

package org.springframework.cloud.appbroker.integration.fixtures;

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServiceKey;
import org.springframework.cloud.appbroker.deployer.BackingServiceKeys;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.util.CollectionUtils;

abstract class AbstractServiceInstanceWorkflow {

	@SuppressWarnings("WeakerAccess")
	final BrokeredServices brokeredServices;

	AbstractServiceInstanceWorkflow(BrokeredServices brokeredServices) {
		this.brokeredServices = brokeredServices;
	}

	Mono<Boolean> accept(ServiceDefinition serviceDefinition, Plan plan) {
		return getBackingServicesForService(serviceDefinition, plan)
				.map(backingServices -> !backingServices.isEmpty())
				.defaultIfEmpty(false);
	}

	TargetSpec getTargetForService(ServiceDefinition serviceDefinition, Plan plan) {
		BrokeredService brokeredService = findBrokeredService(serviceDefinition, plan);
		return brokeredService == null ? null : brokeredService.getTarget();
	}

	Mono<List<BackingService>> getBackingServicesForService(ServiceDefinition serviceDefinition, Plan plan) {
		return Mono.defer(() ->
			Mono.justOrEmpty(findBackingServices(serviceDefinition, plan)));
	}

	private BackingServices findBackingServices(ServiceDefinition serviceDefinition,
												Plan plan) {
		BrokeredService brokeredService = findBrokeredService(serviceDefinition, plan);
		BackingServices backingServices = null;
		if (brokeredService != null && !CollectionUtils.isEmpty(brokeredService.getServices())) {
			backingServices = BackingServices.builder()
				.backingServices(brokeredService.getServices())
				.build();
		}
		return backingServices;
	}

	private BrokeredService findBrokeredService(ServiceDefinition serviceDefinition,
												Plan plan) {
		String serviceName = serviceDefinition.getName();
		String planName = plan.getName();

		return brokeredServices.stream()
							   .filter(brokeredService ->
								   brokeredService.getServiceName().equals(serviceName)
									   && brokeredService.getPlanName().equals(planName))
							   .findFirst()
							   .orElse(null);
	}

	Mono<List<BackingServiceKey>> setServiceKeyName(BackingServiceKeys backingServiceKeys, String bindingId) {
		return Flux.fromIterable(backingServiceKeys).
			flatMap(backingServiceKey -> {
				backingServiceKey.setName(bindingId);
				return Mono.just(backingServiceKey);
			}).collectList();
	}
}
