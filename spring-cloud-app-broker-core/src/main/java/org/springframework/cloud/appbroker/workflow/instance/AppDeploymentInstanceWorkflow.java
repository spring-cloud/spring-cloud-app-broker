/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.workflow.instance;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

class AppDeploymentInstanceWorkflow {

	final BrokeredServices brokeredServices;

	AppDeploymentInstanceWorkflow(BrokeredServices brokeredServices) {
		this.brokeredServices = brokeredServices;
	}

	Mono<Boolean> accept(ServiceDefinition serviceDefinition, String planId) {
		return getBackingApplicationsForService(serviceDefinition, planId)
			.map(backingApplications -> !backingApplications.isEmpty())
			.defaultIfEmpty(false);
	}

	Mono<List<BackingApplication>> getBackingApplicationsForService(ServiceDefinition serviceDefinition, String planId) {
		return Mono.defer(() ->
			Mono.justOrEmpty(findBackingApplications(serviceDefinition, planId)));
	}

	Mono<List<BackingService>> getBackingServicesForService(ServiceDefinition serviceDefinition, String planId) {
		return Mono.defer(() ->
			Mono.justOrEmpty(findBackingServices(serviceDefinition, planId)));
	}

	private BackingApplications findBackingApplications(ServiceDefinition serviceDefinition,
														String planId) {
		BrokeredService brokeredService = findBrokeredService(serviceDefinition, planId);
		return brokeredService == null ? null : new BackingApplications(brokeredService.getApps());
	}

	private BackingServices findBackingServices(ServiceDefinition serviceDefinition,
												String planId) {
		BrokeredService brokeredService = findBrokeredService(serviceDefinition, planId);
		return brokeredService == null || brokeredService.getServices() == null
			? null
			: new BackingServices(brokeredService.getServices());
	}

	private BrokeredService findBrokeredService(ServiceDefinition serviceDefinition,
												String planId) {
		String serviceName = serviceDefinition.getName();

		String planName = serviceDefinition.getPlans().stream()
										   .filter(plan -> plan.getId().equals(planId))
										   .findFirst().get().getName();

		return brokeredServices.stream()
							   .filter(brokeredService ->
								   brokeredService.getServiceName().equals(serviceName)
									   && brokeredService.getPlanName().equals(planName))
							   .findFirst()
							   .orElse(null);
	}
}
