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
import org.springframework.cloud.appbroker.deployer.GetApplicationRequest;
import org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.ServicesSpec;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;

public class BackingAppManagementService {

	private static final Logger LOG = Loggers.getLogger(BackingAppManagementService.class);

	private static final String BACKINGAPPS_LOG_TEMPLATE = "backingApp={}";

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

	/**
	 * Helper method that fetches service name and plan name from Cloud Foundry Service Instances API (CF API) and
	 * invokes {@code stop(serviceInstanceId, serviceName, planName)}.
	 *
	 * Because this method will try to fetch user-created service instance, UAA client used by the broker has to be a
	 * space developer of the space containing the service instance, or has to have {@code cloud_controller.admin}
	 * authority. If you want to avoid CF API call, use
	 * {@link BackingAppManagementService#stop(String, String, String)} method.
	 *
	 * @param serviceInstanceId target service instance id
	 * @return completes when the operation is completed
	 */
	public Mono<Void> stop(String serviceInstanceId) {
		return fetchServiceDetailsAndInvoke(serviceInstanceId, this::stop);
	}


	/**
	 * Stops the backing applications for the service instance with the given id
	 * @param serviceInstanceId target service instance id
	 * @param serviceName service name
	 * @param planName plan name
	 * @return completes when the operation is completed
	 */
	public Mono<Void> stop(String serviceInstanceId, String serviceName, String planName) {
		return getBackingApplicationsForService(serviceInstanceId, serviceName, planName)
			.flatMapMany(backingApps -> Flux.fromIterable(backingApps)
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(managementClient::stop)
				.doOnRequest(l -> {
					LOG.info("Stopping applications");
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				})
				.doOnComplete(() -> {
					LOG.info("Finish stopping applications");
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				})
				.doOnError(e -> {
					LOG.error(String.format("Error stopping applications. error=%s", e.getMessage()), e);
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				}))
			.then();
	}

	/**
	 * Helper method that fetches service name and plan name from Cloud Foundry Service Instances API (CF API) and
	 * invokes {@code start(serviceInstanceId, serviceName, planName)}.
	 *
	 * Because this method will try to fetch user-created service instance, UAA client used by the broker has to be a
	 * space developer of the space containing the service instance, or has to have {@code cloud_controller.admin}
	 * authority. If you want to avoid CF API call, use
	 * {@link BackingAppManagementService#start(String, String, String)} method.
	 *
	 * @param serviceInstanceId target service instance id
	 * @return completes when the operation is completed
	 */
	public Mono<Void> start(String serviceInstanceId) {
		return fetchServiceDetailsAndInvoke(serviceInstanceId, this::start);
	}

	/**
	 * Starts the backing applications for the service instance with the given id
	 * @param serviceInstanceId target service instance id
	 * @param serviceName service name
	 * @param planName plan name
	 * @return completes when the operation is completed
	 */
	public Mono<Void> start(String serviceInstanceId, String serviceName, String planName) {
		return getBackingApplicationsForService(serviceInstanceId, serviceName, planName)
			.flatMapMany(backingApps -> Flux.fromIterable(backingApps)
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(managementClient::start)
				.doOnRequest(l -> {
					LOG.info("Starting applications");
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				})
				.doOnComplete(() -> {
					LOG.info("Finish starting applications");
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				})
				.doOnError(e -> {
					LOG.error(String.format("Error starting applications. error=%s", e.getMessage()), e);
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				}))
			.then();
	}

	/**
	 * Helper method that fetches service name and plan name from Cloud Foundry Service Instances API (CF API) and
	 * invokes {@code restart(serviceInstanceId, serviceName, planName)}.
	 *
	 * Because this method will try to fetch user-created service instance, UAA client used by the broker has to be a
	 * space developer of the space containing the service instance, or has to have {@code cloud_controller.admin}
	 * authority. If you want to avoid CF API call, use
	 * {@link BackingAppManagementService#restart(String, String, String)} method.
	 *
	 * @param serviceInstanceId target service instance id
	 * @return completes when the operation is completed
	 */
	public Mono<Void> restart(String serviceInstanceId) {
		return fetchServiceDetailsAndInvoke(serviceInstanceId, this::restart);
	}

	/**
	 * Restarts the backing applications for the service instance with the given id
	 * @param serviceInstanceId target service instance id
	 * @param serviceName service name
	 * @param planName plan name
	 * @return completes when the operation is completed
	 */
	public Mono<Void> restart(String serviceInstanceId, String serviceName, String planName) {
		return getBackingApplicationsForService(serviceInstanceId, serviceName, planName)
			.flatMapMany(backingApps -> Flux.fromIterable(backingApps)
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(managementClient::restart)
				.doOnRequest(l -> {
					LOG.info("Restarting applications");
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				})
				.doOnComplete(() -> {
					LOG.info("Finish restarting applications");
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				})
				.doOnError(e -> {
					LOG.error(String.format("Error restarting applications. error=%s", e.getMessage()), e);
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				}))
			.then();
	}

	/**
	 * Helper method that fetches service name and plan name from Cloud Foundry Service Instances API (CF API) and
	 * invokes {@code restage(serviceInstanceId, serviceName, planName)}.
	 *
	 * Because this method will try to fetch user-created service instance, UAA client used by the broker has to be a
	 * space developer of the space containing the service instance, or has to have {@code cloud_controller.admin}
	 * authority. If you want to avoid CF API call, use
	 * {@link BackingAppManagementService#restage(String, String, String)} method.
	 *
	 * @param serviceInstanceId target service instance id
	 * @return completes when the operation is completed
	 */
	public Mono<Void> restage(String serviceInstanceId) {
		return fetchServiceDetailsAndInvoke(serviceInstanceId, this::restage);
	}

	/**
	 * Restages the backing applications for the service instance with the given id
	 * @param serviceInstanceId target service instance id
	 * @param serviceName service name
	 * @param planName plan name
	 * @return completes when the operation is completed
	 */
	public Mono<Void> restage(String serviceInstanceId, String serviceName, String planName) {
		return getBackingApplicationsForService(serviceInstanceId, serviceName, planName)
			.flatMapMany(backingApps -> Flux.fromIterable(backingApps)
				.parallel()
				.runOn(Schedulers.parallel())
				.flatMap(managementClient::restage)
				.doOnRequest(l -> {
					LOG.info("Restaging applications");
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				})
				.doOnComplete(() -> {
					LOG.info("Finish restaging applications");
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				})
				.doOnError(e -> {
					LOG.error(String.format("Error restaging applications. error=%s", e.getMessage()), e);
					LOG.debug(BACKINGAPPS_LOG_TEMPLATE, backingApps);
				}))
			.then();
	}

	/**
	 * Helper method that fetches service name and plan name from Cloud Foundry Service Instances API (CF API) and
	 * invokes {@code getDeployedBackingApplications(serviceInstanceId, serviceName, planName)}.
	 *
	 * Because this method will try to fetch user-created service instance, UAA client used by the broker has to be a
	 * space developer of the space containing the service instance, or has to have {@code cloud_controller.admin}
	 * authority. If you want to avoid CF API call, use
	 * {@link BackingAppManagementService#getDeployedBackingApplications(String, String, String)} method.
	 *
	 * @param serviceInstanceId target service instance id
	 * @return backing applications for the target service instance
	 */
	public Mono<BackingApplications> getDeployedBackingApplications(String serviceInstanceId) {
		return fetchServiceDetailsAndInvoke(serviceInstanceId, this::getDeployedBackingApplications);
	}

	/**
	 * Returns a list of backing applications for the service instance with the given id
	 * @param serviceInstanceId target service instance id
	 * @param serviceName service name
	 * @param planName plan name
	 * @return backing applications for the target service instance
	 */
	public Mono<BackingApplications> getDeployedBackingApplications(String serviceInstanceId, String serviceName, String planName) {
		return getBackingApplicationsForService(serviceInstanceId, serviceName, planName)
			.flatMapMany(Flux::fromIterable)
			.flatMap(app ->
				appDeployer
					.get(GetApplicationRequest.builder()
						.name(app.getName())
						.properties(app.getProperties())
						.build())
					.flatMap(response -> Flux.fromIterable(response.getServices())
						.map(boundServiceName ->
							ServicesSpec.builder()
								.serviceInstanceName(boundServiceName)
								.build())
						.collectList()
						.map(services -> BackingApplication
							.builder()
							.name(response.getName())
							.services(services)
							.environment(response.getEnvironment())
							.build()))
					.doOnRequest(l -> {
						LOG.info("Getting deployed backing application. appName={}", app.getName());
						LOG.debug("backingApp={}", app);
					})
					.doOnError(e -> {
						LOG.error(String.format("Error getting deployed backing application. appName=%s, error=%s",
							app.getName(), e.getMessage()), e);
						LOG.debug("backingApp={}", app);
					})
					.onErrorResume(exception -> Mono.empty()))
			.collectList()
			.map(BackingApplications::new)
			.doOnSuccess(backingApplications -> LOG.debug("backingApplications={}", backingApplications));
	}

	public Mono<BackingApplications> getBackingApplicationsForService(String serviceInstanceId, String serviceName,
		String planName) {
		return findBrokeredService(serviceName, planName)
			.flatMap(brokeredService -> updateBackingApps(brokeredService, serviceInstanceId))
			.map(backingApplications -> BackingApplications.builder().backingApplications(backingApplications).build());
	}

	private <T> Mono<T> fetchServiceDetailsAndInvoke(String serviceInstanceId, BackingAppAction<T> action) {
		return appDeployer
			.getServiceInstance(GetServiceInstanceRequest.builder().serviceInstanceId(serviceInstanceId).build())
			.flatMap(serviceInstance -> action.invoke(serviceInstanceId, serviceInstance.getService(),
				serviceInstance.getPlan()));
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

	@FunctionalInterface
	private interface BackingAppAction<T> {
		Mono<T> invoke(String serviceInstanceId, String serviceName, String planName);
	}

}
