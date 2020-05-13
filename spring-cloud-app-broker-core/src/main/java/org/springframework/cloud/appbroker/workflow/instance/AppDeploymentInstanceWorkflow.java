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

package org.springframework.cloud.appbroker.workflow.instance;

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

public class AppDeploymentInstanceWorkflow {

	private final BrokeredServices brokeredServices;

	protected AppDeploymentInstanceWorkflow(BrokeredServices brokeredServices) {
		this.brokeredServices = brokeredServices;
	}

	protected Mono<Boolean> accept(ServiceDefinition serviceDefinition, Plan plan) {
		return getBackingApplicationsForService(serviceDefinition, plan)
			.map(backingApplications -> !backingApplications.isEmpty())
			.filter(Boolean::booleanValue) // filter out Boolean.False to proceed with flux, see https://stackoverflow.com/questions/49860558/project-reactor-conditional-execution
			.switchIfEmpty(
				getBackingServicesForService(serviceDefinition, plan)
				.map(backingServices -> !backingServices.isEmpty())
				.defaultIfEmpty(false)
			);
	}

	protected Mono<TargetSpec> getTargetForService(ServiceDefinition serviceDefinition, Plan plan) {
		return findBrokeredService(serviceDefinition, plan)
			.flatMap(brokeredService -> Mono.justOrEmpty(brokeredService.getTarget()));
	}

	protected Mono<List<BackingApplication>> getBackingApplicationsForService(ServiceDefinition serviceDefinition,
		Plan plan) {
		return findBrokeredService(serviceDefinition, plan)
			.flatMap(brokeredService -> Mono.justOrEmpty(brokeredService.getApps()))
			.map(backingApplications -> BackingApplications.builder()
				.backingApplications(backingApplications)
				.build());
	}

	protected Mono<List<BackingService>> getBackingServicesForService(ServiceDefinition serviceDefinition, Plan plan) {
		return findBrokeredService(serviceDefinition, plan)
			.flatMap(brokeredService -> Mono.justOrEmpty(brokeredService.getServices()))
			.map(backingServices -> BackingServices.builder()
				.backingServices(backingServices)
				.build());
	}

	private Mono<BrokeredService> findBrokeredService(ServiceDefinition serviceDefinition, Plan plan) {
		return Flux.fromIterable(brokeredServices)
			.filter(brokeredService -> brokeredService.getServiceName().equals(serviceDefinition.getName())
				&& brokeredService.getPlanName().equals(plan.getName()))
			.singleOrEmpty();
	}

}
