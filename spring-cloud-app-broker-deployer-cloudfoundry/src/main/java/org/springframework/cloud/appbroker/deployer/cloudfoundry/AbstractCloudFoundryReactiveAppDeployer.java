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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import org.cloudfoundry.AbstractCloudFoundryException;
import org.cloudfoundry.UnknownCloudFoundryException;
import org.cloudfoundry.util.DelayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.util.ByteSizeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.BUILDPACK_PROPERTY_KEY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.JAVA_OPTS_PROPERTY_KEY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.SERVICES_PROPERTY_KEY;

class AbstractCloudFoundryReactiveAppDeployer {

	private final RuntimeEnvironmentInfo runtimeEnvironmentInfo;

	final CloudFoundryDeploymentProperties deploymentProperties;

	private final Logger logger = LoggerFactory.getLogger(AbstractCloudFoundryReactiveAppDeployer.class);

	AbstractCloudFoundryReactiveAppDeployer(CloudFoundryDeploymentProperties deploymentProperties, RuntimeEnvironmentInfo runtimeEnvironmentInfo) {
		this.deploymentProperties = deploymentProperties;
		this.runtimeEnvironmentInfo = runtimeEnvironmentInfo;
	}

	int memory(AppDeploymentRequest request) {
		String withUnit = request.getDeploymentProperties()
				.getOrDefault(org.springframework.cloud.deployer.spi.app.AppDeployer.MEMORY_PROPERTY_KEY, this.deploymentProperties.getMemory());
		return (int) ByteSizeUtils.parseToMebibytes(withUnit);
	}

	Set<String> servicesToBind(AppDeploymentRequest request) {
		Set<String> services = new HashSet<>();
		services.addAll(this.deploymentProperties.getServices());
		services.addAll(StringUtils.commaDelimitedListToSet(request.getDeploymentProperties().get(SERVICES_PROPERTY_KEY)));
		return services;
	}

	int diskQuota(AppDeploymentRequest request) {
		String withUnit = request.getDeploymentProperties()
				.getOrDefault(AppDeployer.DISK_PROPERTY_KEY, this.deploymentProperties.getDisk());
		return (int) ByteSizeUtils.parseToMebibytes(withUnit);
	}

	String buildpack(AppDeploymentRequest request) {
		return Optional.ofNullable(request.getDeploymentProperties().get(BUILDPACK_PROPERTY_KEY))
				.orElse(this.deploymentProperties.getBuildpack());
	}

	String javaOpts(AppDeploymentRequest request) {
		return Optional.ofNullable(request.getDeploymentProperties().get(JAVA_OPTS_PROPERTY_KEY))
				.orElse(this.deploymentProperties.getJavaOpts());
	}

	Predicate<Throwable> isNotFoundError() {
		return t -> t instanceof AbstractCloudFoundryException && ((AbstractCloudFoundryException) t).getStatusCode() == HttpStatus.NOT_FOUND.value();
	}

	/**
	 * Return a Docker image identifier if the application Resource is for a Docker image, or {@literal null} otherwise.
	 *
	 * @see #getApplication(AppDeploymentRequest)
	 */
	String getDockerImage(AppDeploymentRequest request) {
		try {
			String uri = request.getResource().getURI().toString();
			if (uri.startsWith("docker:")) {
				return uri.substring("docker:".length());
			} else {
				return null;
			}
		} catch (IOException e) {
			throw Exceptions.propagate(e);
		}
	}

	/**
	 * Return a Path to the application Resource or {@literal null} if the request is for a Docker image.
	 *
	 * @see #getDockerImage(AppDeploymentRequest)
	 */
	Path getApplication(AppDeploymentRequest request) {
		try {
			if (!request.getResource().getURI().toString().startsWith("docker:")) {
				return request.getResource().getFile().toPath();
			} else {
				return null;
			}
		} catch (IOException e) {
			throw Exceptions.propagate(e);
		}
	}

	/**
	 * Return a function usable in {@literal doOnError} constructs that will unwrap unrecognized Cloud Foundry Exceptions
	 * and log the text payload.
	 */
	protected Consumer<Throwable> logError(String msg) {
		return e -> {
			if (e instanceof UnknownCloudFoundryException) {
				logger.error(msg + "\nUnknownCloudFoundryException encountered, whose payload follows:\n" + ((UnknownCloudFoundryException)e).getPayload(), e);
			} else {
				logger.error(msg, e);
			}
		};
	}


	/**
	 * To be used in order to retry the status operation for an application or task.
	 * @param id The application id or the task id
	 * @param <T> The type of status object being queried for, usually AppStatus or TaskStatus
	 * @return The function that executes the retry logic around for determining App or Task Status
	 */
	<T> Function<Mono<T>, Mono<T>> statusRetry(String id) {
		long statusTimeout = this.deploymentProperties.getStatusTimeout();
		long requestTimeout = Math.round(statusTimeout * 0.40); // wait 500ms with default status timeout of 2000ms
		long initialRetryDelay = Math.round(statusTimeout * 0.10); // wait 200ms with status timeout of 2000ms

		if (requestTimeout < 500L) {
			logger.info("Computed statusRetry Request timeout = {} ms is below 500ms minimum value.  Setting to 500ms", requestTimeout);
			requestTimeout = 500L;
		}
		final long requestTimeoutToUse = requestTimeout;
		return m -> m.timeout(Duration.ofMillis(requestTimeoutToUse))
				.doOnError(e -> logger.warn(String.format("Error getting status for %s within %sms, Retrying operation.", id, requestTimeoutToUse)))
				.retryWhen(DelayUtils.exponentialBackOffError(
						Duration.ofMillis(initialRetryDelay), //initial retry delay
						Duration.ofMillis(statusTimeout / 2), // max retry delay
						Duration.ofMillis(statusTimeout)) // max total retry time
						.andThen(retries -> Flux.from(retries).doOnComplete(() ->
								logger.info("Successfully retried getStatus operation status [{}] for {}", id))))
				.doOnError(e -> logger.error(String.format("Retry operation on getStatus failed for %s.  Max retry time %sms", id, statusTimeout)));
	}


	public RuntimeEnvironmentInfo environmentInfo() {
		return runtimeEnvironmentInfo;
	}
}