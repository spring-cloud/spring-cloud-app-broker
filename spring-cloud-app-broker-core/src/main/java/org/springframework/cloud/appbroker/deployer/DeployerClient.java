/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.stream.Collectors;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class DeployerClient {

	private static final Logger LOG = Loggers.getLogger(DeployerClient.class);

	private final AppDeployer appDeployer;

	public DeployerClient(AppDeployer appDeployer) {
		this.appDeployer = appDeployer;
	}

	public Mono<String> deploy(BackingApplication backingApplication, String serviceInstanceId) {
		return appDeployer
			.deploy(DeployApplicationRequest
				.builder()
				.name(backingApplication.getName())
				.path(backingApplication.getPath())
				.properties(backingApplication.getProperties())
				.environment(backingApplication.getEnvironment())
				.services(backingApplication.getServices().stream()
					.map(ServicesSpec::getServiceInstanceName)
					.collect(Collectors.toList()))
				.serviceInstanceId(serviceInstanceId)
				.build())
			.doOnRequest(l -> {
				LOG.info("Deploying application. backingAppName={}", backingApplication.getName());
				debugLog(backingApplication);
			})
			.doOnSuccess(response -> {
				LOG.info("Success deploying application. backingAppName={}", backingApplication.getName());
				debugLog(response, backingApplication);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error deploying application. backingAppName=%s, error=%s",
					backingApplication.getName(), e.getMessage()), e);
				debugLog(backingApplication);
			})
			.map(DeployApplicationResponse::getName);
	}

	public Mono<String> preUpdate(BackingApplication backingApplication, String serviceInstanceId) {
		return appDeployer.preUpdate(getUpdateApplicationRequest(backingApplication, serviceInstanceId))
			.doOnRequest(l -> {
				LOG.info("Pre-updating application. backingAppName={}", backingApplication.getName());
				debugLog(backingApplication);
			})
			.doOnSuccess(response -> {
				LOG.info("Success pre-updating application. backingAppName={}", backingApplication.getName());
				debugLog(response, backingApplication);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error pre-updating application. backingAppName=%s, error=%s",
					backingApplication.getName(), e.getMessage()), e);
				debugLog(backingApplication);
			})
			.map(UpdateApplicationResponse::getName);
	}

	public Mono<String> update(BackingApplication backingApplication, String serviceInstanceId) {
		return appDeployer
			.update(getUpdateApplicationRequest(backingApplication, serviceInstanceId))
			.doOnRequest(l -> {
				LOG.info("Updating application. backingAppName={}", backingApplication.getName());
				debugLog(backingApplication);
			})
			.doOnSuccess(response -> {
				LOG.info("Success updating application. backingAppName={}", backingApplication.getName());
				debugLog(response, backingApplication);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error updating application. backingAppName=%s, error=%s",
					backingApplication.getName(), e.getMessage()), e);
				debugLog(backingApplication);
			})
			.map(UpdateApplicationResponse::getName);
	}

	public Mono<String> undeploy(BackingApplication backingApplication) {
		return appDeployer
			.undeploy(UndeployApplicationRequest
				.builder()
				.properties(backingApplication.getProperties())
				.name(backingApplication.getName())
				.build())
			.doOnRequest(l -> {
				LOG.info("Undeploying application. backingAppName={}", backingApplication.getName());
				debugLog(backingApplication);
			})
			.doOnSuccess(response -> {
				LOG.info("Success undeploying application. backingAppName={}", backingApplication.getName());
				debugLog(response, backingApplication);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error undeploying application. backingAppName=%s, error=%s",
					backingApplication.getName(), e.getMessage()), e);
				debugLog(backingApplication);
			})
			.onErrorReturn(UndeployApplicationResponse.builder()
				.name(backingApplication.getName())
				.build())
			.map(UndeployApplicationResponse::getName);
	}

	public Mono<String> createServiceInstance(BackingService backingService) {
		return appDeployer
			.createServiceInstance(
				CreateServiceInstanceRequest
					.builder()
					.serviceInstanceName(backingService.getServiceInstanceName())
					.name(backingService.getName())
					.plan(backingService.getPlan())
					.parameters(backingService.getParameters())
					.properties(backingService.getProperties())
					.build())
			.doOnRequest(l -> {
				LOG.info("Creating backing service {}", backingService.getName());
				debugLog(backingService);
			})
			.doOnSuccess(response -> {
				LOG.info("Success creating backing service {}", backingService.getName());
				debugLog(response, backingService);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error creating backing service. backingServiceName=%s, error=%s",
					backingService.getName(), e.getMessage()), e);
				debugLog(backingService);
			})
			.map(CreateServiceInstanceResponse::getName);
	}

	public Mono<String> updateServiceInstance(BackingService backingService) {
		return appDeployer
			.updateServiceInstance(
				UpdateServiceInstanceRequest
					.builder()
					.serviceInstanceName(backingService.getServiceInstanceName())
					.parameters(backingService.getParameters())
					.properties(backingService.getProperties())
					.rebindOnUpdate(backingService.isRebindOnUpdate())
					.build())
			.doOnRequest(l -> {
				LOG.info("Updating backing service {}", backingService.getName());
				debugLog(backingService);
			})
			.doOnSuccess(response -> {
				LOG.info("Success updating backing service {}", backingService.getName());
				debugLog(response, backingService);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error updating backing service. backingServiceName=%s, error=%s",
					backingService.getName(), e.getMessage()), e);
				debugLog(backingService);
			})
			.map(UpdateServiceInstanceResponse::getName);
	}

	public Mono<String> deleteServiceInstance(BackingService backingService) {
		return appDeployer
			.deleteServiceInstance(
				DeleteServiceInstanceRequest
					.builder()
					.serviceInstanceName(backingService.getServiceInstanceName())
					.properties(backingService.getProperties())
					.build())
			.doOnRequest(l -> {
				LOG.info("Deleting backing service {}", backingService.getName());
				debugLog(backingService);
			})
			.doOnSuccess(response -> {
				LOG.info("Success deleting backing service {}", backingService.getName());
				debugLog(response, backingService);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error deleting backing service. backingServiceName=%s, error=%s",
					backingService.getName(), e.getMessage()), e);
				debugLog(backingService);
			})
			.onErrorReturn(DeleteServiceInstanceResponse.builder()
				.name(backingService.getServiceInstanceName())
				.build())
			.map(DeleteServiceInstanceResponse::getName);
	}

	public Mono<String> deleteSpace(String spaceName) {
		return appDeployer
			.deleteBackingSpace(
				DeleteBackingSpaceRequest
					.builder()
					.name(spaceName)
					.build())
			.doOnRequest(l -> {
				LOG.info("Deleting backing space {}", spaceName);
			})
			.doOnSuccess(response -> {
				LOG.info("Success deleting backing space {}", spaceName);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error deleting backing space. backingSpaceName=%s, error=%s",
					spaceName, e.getMessage()), e);
			})
			.onErrorReturn(DeleteBackingSpaceResponse.builder()
				.name(spaceName)
				.build())
			.map(DeleteBackingSpaceResponse::getName);
	}

	private static UpdateApplicationRequest getUpdateApplicationRequest(BackingApplication backingApplication,
		String serviceInstanceId) {
		return UpdateApplicationRequest
			.builder()
			.name(backingApplication.getName())
			.path(backingApplication.getPath())
			.properties(backingApplication.getProperties())
			.environment(backingApplication.getEnvironment())
			.services(backingApplication.getServices().stream()
				.map(ServicesSpec::getServiceInstanceName)
				.collect(Collectors.toList()))
			.serviceInstanceId(serviceInstanceId)
			.build();
	}

	private static void debugLog(BackingApplication backingApplication) {
		LOG.debug("backingApp={}", backingApplication);
	}

	private static void debugLog(BackingService backingService) {
		LOG.debug("backingService={}", backingService);
	}

	private static void debugLog(Object response, BackingApplication backingApplication) {
		LOG.debug("response={}, backingApp={}", response, backingApplication);
	}

	private static void debugLog(Object response, BackingService backingService) {
		LOG.debug("response={}, backingService={}", response, backingService);
	}
}
