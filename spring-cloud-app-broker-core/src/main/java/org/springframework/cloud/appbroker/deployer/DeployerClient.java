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

package org.springframework.cloud.appbroker.deployer;

import java.util.stream.Collectors;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class DeployerClient {

	private static final Logger LOG = Loggers.getLogger(DeployerClient.class);

	private static final String BACKINGAPP_LOG_TEMPLATE = "backingApp={}";

	private static final String BACKINGSERVICE_LOG_TEMPLATE = "backingService={}";

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
				LOG.debug(BACKINGAPP_LOG_TEMPLATE, backingApplication);
			})
			.doOnSuccess(response -> {
				LOG.info("Success deploying application. backingAppName={}", backingApplication.getName());
				LOG.debug("response={}, backingApp={}", response, backingApplication);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error deploying application. backingAppName=%s, error=%s",
					backingApplication.getName(), e.getMessage()), e);
				LOG.debug(BACKINGAPP_LOG_TEMPLATE, backingApplication);
			})
			.map(DeployApplicationResponse::getName);
	}

	public Mono<String> update(BackingApplication backingApplication, String serviceInstanceId) {
		return appDeployer
			.update(UpdateApplicationRequest
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
				LOG.info("Updating application. backingAppName={}", backingApplication.getName());
				LOG.debug(BACKINGAPP_LOG_TEMPLATE, backingApplication);
			})
			.doOnSuccess(response -> {
				LOG.info("Success updating application. backingAppName={}", backingApplication.getName());
				LOG.debug("response={}, backingApp={}", response, backingApplication);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error updating application. backingAppName=%s, error=%s",
					backingApplication.getName(), e.getMessage()), e);
				LOG.debug(BACKINGAPP_LOG_TEMPLATE, backingApplication);
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
				LOG.debug(BACKINGAPP_LOG_TEMPLATE, backingApplication);
			})
			.doOnSuccess(response -> {
				LOG.info("Success undeploying application. backingAppName={}", backingApplication.getName());
				LOG.debug("response={}, backingApp={}", response, backingApplication);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error undeploying application. backingAppName=%s, error=%s",
					backingApplication.getName(), e.getMessage()), e);
				LOG.debug(BACKINGAPP_LOG_TEMPLATE, backingApplication);
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
				LOG.debug(BACKINGSERVICE_LOG_TEMPLATE, backingService);
			})
			.doOnSuccess(response -> {
				LOG.info("Success creating backing service {}", backingService.getName());
				LOG.debug("response={}, backingService={}", response, backingService);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error creating backing service. backingServiceName=%s, error=%s",
					backingService.getName(), e.getMessage()), e);
				LOG.debug(BACKINGSERVICE_LOG_TEMPLATE, backingService);
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
				LOG.debug(BACKINGSERVICE_LOG_TEMPLATE, backingService);
			})
			.doOnSuccess(response -> {
				LOG.info("Success updating backing service {}", backingService.getName());
				LOG.debug("response={}, backingService={}", response, backingService);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error updating backing service. backingServiceName=%s, error=%s",
					backingService.getName(), e.getMessage()), e);
				LOG.debug(BACKINGSERVICE_LOG_TEMPLATE, backingService);
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
				LOG.debug(BACKINGSERVICE_LOG_TEMPLATE, backingService);
			})
			.doOnSuccess(response -> {
				LOG.info("Success deleting backing service {}", backingService.getName());
				LOG.debug("response={}, backingService={}", response, backingService);
			})
			.doOnError(e -> {
				LOG.error(String.format("Error deleting backing service. backingServiceName=%s, error=%s",
					backingService.getName(), e.getMessage()), e);
				LOG.debug(BACKINGSERVICE_LOG_TEMPLATE, backingService);
			})
			.onErrorReturn(DeleteServiceInstanceResponse.builder()
				.name(backingService.getServiceInstanceName())
				.build())
			.map(DeleteServiceInstanceResponse::getName);
	}

}
