/*
 * Copyright 2002-2019 the original author or authors
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

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;

public class BackingAppManagementService {

	private final Logger log = Loggers.getLogger(BackingAppManagementService.class);

	private final ManagementClient managementClient;

	private final AppDeployer appDeployer;

	private final BrokeredServices brokeredServices;

	private final TargetService targetService;

	public BackingAppManagementService(ManagementClient managementClient, AppDeployer appDeployer,
		BrokeredServices brokeredServices, TargetService targetService) {
		this.managementClient = managementClient;
		this.appDeployer = appDeployer;
		this.brokeredServices = brokeredServices;
		this.targetService = targetService;
	}

	public Mono<Void> stop(String serviceInstanceId) {
		return getBackingApplicationsForService(serviceInstanceId)
			.flatMapMany(backingApps -> Flux.fromIterable(backingApps)
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(managementClient::stop)
				.doOnRequest(l -> log.debug("Stopping applications {}", backingApps))
				.doOnEach(response -> log.debug("Finished stopping application {}", response))
				.doOnComplete(() -> log.debug("Finished stopping application {}", backingApps))
				.doOnError(exception -> log.error(String.format("Error stopping applications %s with error '%s'",
					backingApps, exception.getMessage()), exception)))
			.then();
	}

	public Mono<Void> start(String serviceInstanceId) {
		return getBackingApplicationsForService(serviceInstanceId)
			.flatMapMany(backingApps -> Flux.fromIterable(backingApps)
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(managementClient::start)
				.doOnRequest(l -> log.debug("Starting applications {}", backingApps))
				.doOnEach(response -> log.debug("Finished starting application {}", response))
				.doOnComplete(() -> log.debug("Finished starting application {}", backingApps))
				.doOnError(exception -> log.error(String.format("Error starting applications %s with error '%s'",
					backingApps, exception.getMessage()), exception)))
			.then();
	}

	public Mono<Void> restart(String serviceInstanceId) {
		return getBackingApplicationsForService(serviceInstanceId)
			.flatMapMany(backingApps -> Flux.fromIterable(backingApps)
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(managementClient::restart)
				.doOnRequest(l -> log.debug("Restarting applications {}", backingApps))
				.doOnEach(response -> log.debug("Finished restarting application {}", response))
				.doOnComplete(() -> log.debug("Finished restarting application {}", backingApps))
				.doOnError(exception -> log.error(String.format("Error restarting applications %s with error '%s'",
					backingApps, exception.getMessage()), exception)))
			.then();
	}

	public Mono<Void> restage(String serviceInstanceId) {
		return getBackingApplicationsForService(serviceInstanceId)
			.flatMapMany(backingApps -> Flux.fromIterable(backingApps)
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(managementClient::restage)
				.doOnRequest(l -> log.debug("Restaging applications {}", backingApps))
				.doOnEach(response -> log.debug("Finished restaging application {}", response))
				.doOnComplete(() -> log.debug("Finished restaging application {}", backingApps))
				.doOnError(exception -> log.error(String.format("Error restaging applications %s with error '%s'",
					backingApps, exception.getMessage()), exception)))
			.then();
	}

	private Mono<List<BackingApplication>> getBackingApplicationsForService(String serviceInstanceId) {
		return appDeployer.getServiceInstance(GetServiceInstanceRequest.builder()
			.serviceInstanceId(serviceInstanceId)
			.build())
			.flatMap(response -> findBrokeredService(response.getService(), response.getPlan()))
			.flatMap(brokeredService -> updateBackingApps(brokeredService, serviceInstanceId));
	}

	private Mono<BrokeredService> findBrokeredService(String serviceName, String planName) {
		return Flux.fromIterable(brokeredServices)
			.filter(brokeredService -> brokeredService.getServiceName().equals(serviceName)
				&& brokeredService.getPlanName().equals(planName))
			.singleOrEmpty();
	}

	private Mono<List<BackingApplication>> updateBackingApps(BrokeredService brokeredService,
		String serviceInstanceId) {
		return Mono.just(BackingApplications.builder()
			.backingApplications(brokeredService.getApps())
			.build())
			.flatMap(backingApps -> targetService.addToBackingApplications(backingApps,
				brokeredService.getTarget(), serviceInstanceId));
	}

}
