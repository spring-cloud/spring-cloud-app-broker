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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.AbstractCloudFoundryException;
import org.cloudfoundry.UnknownCloudFoundryException;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.DefaultApplications;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.Docker;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.Route;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationInfoRequest;
import org.cloudfoundry.operations.spaces.CreateSpaceRequest;
import org.cloudfoundry.operations.spaces.DefaultSpaces;
import org.cloudfoundry.operations.spaces.DeleteSpaceRequest;
import org.cloudfoundry.operations.spaces.GetSpaceRequest;
import org.cloudfoundry.operations.spaces.SpaceDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.DeployApplicationRequest;
import org.springframework.cloud.appbroker.deployer.DeployApplicationResponse;
import org.springframework.cloud.appbroker.deployer.DeploymentProperties;
import org.springframework.cloud.appbroker.deployer.UndeployApplicationRequest;
import org.springframework.cloud.appbroker.deployer.UndeployApplicationResponse;
import org.springframework.cloud.appbroker.deployer.util.ByteSizeUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

@SuppressWarnings("PMD.GodClass")
public class CloudFoundryAppDeployer implements AppDeployer, ResourceLoaderAware {

	private final Logger logger = LoggerFactory.getLogger(CloudFoundryAppDeployer.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final CloudFoundryDeploymentProperties defaultDeploymentProperties;

	private final CloudFoundryOperations operations;

	private ResourceLoader resourceLoader;

	public CloudFoundryAppDeployer(CloudFoundryDeploymentProperties deploymentProperties,
								   CloudFoundryOperations operations,
								   ResourceLoader resourceLoader) {
		this.defaultDeploymentProperties = deploymentProperties;
		this.operations = operations;
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public Mono<DeployApplicationResponse> deploy(DeployApplicationRequest request) {
		String appName = request.getName();
		Resource appResource = getAppResource(request);
		Map<String, String> deploymentProperties = request.getProperties();

		logger.trace("Deploying application: request={}, resource={}",
			appName, appResource);

		return pushApplication(request, deploymentProperties, appResource)
				.timeout(Duration.ofSeconds(this.defaultDeploymentProperties.getApiTimeout()))
				.doOnSuccess(item -> logger.info("Successfully deployed {}", appName))
				.doOnError(error -> {
					if (isNotFoundError().test(error)) {
						logger.warn("Unable to deploy application. It may have been destroyed before start completed: " + error.getMessage());
					}
					else {
						logError(String.format("Failed to deploy %s", appName)).accept(error);
					}
				})
				.thenReturn(DeployApplicationResponse.builder()
					.name(appName)
					.build());
	}

	private Mono<Void> pushApplication(DeployApplicationRequest request,
									   Map<String, String> deploymentProperties,
									   Resource appResource) {
		ApplicationManifest manifest = buildAppManifest(request, deploymentProperties, appResource);

		logger.debug("Pushing manifest" + manifest.toString());

		PushApplicationManifestRequest applicationManifestRequest =
			PushApplicationManifestRequest.builder()
										  .manifest(manifest)
										  .stagingTimeout(this.defaultDeploymentProperties.getStagingTimeout())
										  .startupTimeout(this.defaultDeploymentProperties.getStartupTimeout())
										  .build();

		Mono<Void> requestPushApplication = requestPushApplication(applicationManifestRequest);
		if (deploymentProperties.containsKey(DeploymentProperties.TARGET_KEY)) {
			String space = deploymentProperties.get(DeploymentProperties.TARGET_KEY);
			requestPushApplication = requestPushApplicationInSpace(applicationManifestRequest, space);
		}

		return requestPushApplication
			.doOnSuccess(v -> logger.info("Done uploading bits for {}", request.getName()))
			.doOnError(e -> logger.error(String.format("Error creating app %s.  Exception Message %s", request.getName(), e.getMessage())));
	}

	private ApplicationManifest buildAppManifest(DeployApplicationRequest request,
												 Map<String, String> deploymentProperties,
												 Resource appResource) {
		ApplicationManifest.Builder manifest = ApplicationManifest.builder()
			.name(request.getName())
			.path(getApplication(appResource))
			.environmentVariables(getEnvironmentVariables(request.getEnvironment()))
			.services(request.getServices())
			.instances(instances(deploymentProperties))
			.memory(memory(deploymentProperties))
			.disk(diskQuota(deploymentProperties))
			.healthCheckType(healthCheck(deploymentProperties))
			.healthCheckHttpEndpoint(healthCheckEndpoint(deploymentProperties))
			.timeout(healthCheckTimeout(deploymentProperties))
			.noRoute(toggleNoRoute(deploymentProperties));

		Optional.ofNullable(host(deploymentProperties)).ifPresent(manifest::host);
		Optional.ofNullable(domain(deploymentProperties)).ifPresent(manifest::domain);
		Optional.ofNullable(routePath(deploymentProperties)).ifPresent(manifest::routePath);

		if (route(deploymentProperties) != null) {
			manifest.route(Route.builder().route(route(deploymentProperties)).build());
		}

		if (!routes(deploymentProperties).isEmpty()) {
			Set<Route> routes = routes(deploymentProperties).stream()
					.map(r -> Route.builder().route(r).build())
					.collect(Collectors.toSet());
			manifest.routes(routes);
		}

		if (getDockerImage(appResource) == null) {
			manifest.buildpack(buildpack(deploymentProperties));
		} else {
			manifest.docker(Docker.builder().image(getDockerImage(appResource)).build());
		}
		return manifest.build();
	}

	private Mono<Void> requestPushApplication(PushApplicationManifestRequest request) {
		return this.operations.applications()
			.pushManifest(request);
	}

	private Mono<Void> requestPushApplicationInSpace(PushApplicationManifestRequest request, String space) {
		return createSpaceOperations()
			.create(CreateSpaceRequest.builder().name(space).build())
			.then(createCloudFoundryOperationsForSpace(space).applications().pushManifest(request));
	}

	private DefaultCloudFoundryOperations createCloudFoundryOperationsForSpace(String space) {
		return DefaultCloudFoundryOperations
			.builder()
			.from((DefaultCloudFoundryOperations) this.operations)
			.space(space).build();
	}

	private Mono<String> getOrganizationIdPublisher() {
		OrganizationInfoRequest organizationInfoRequest =
			OrganizationInfoRequest.builder().name(this.defaultDeploymentProperties.getDefaultOrg()).build();
		return this.operations.organizations().get(organizationInfoRequest).map(OrganizationDetail::getId);
	}

	@Override
	public Mono<UndeployApplicationResponse> undeploy(UndeployApplicationRequest request) {
		String appName = request.getName();

		logger.trace("Undeploying application: request={}", request);

		Map<String, String> deploymentProperties = request.getProperties();
		Mono<Void> requestDeleteApplication;
		if (deploymentProperties.containsKey(DeploymentProperties.TARGET_KEY)) {
			String space = deploymentProperties.get(DeploymentProperties.TARGET_KEY);
			requestDeleteApplication = requestDeleteApplicationInSpace(appName, space)
				.then(createSpaceOperations().delete(DeleteSpaceRequest.builder().name(space).build()));
		} else {
			requestDeleteApplication = requestDeleteApplication(appName);
		}

		return
			requestDeleteApplication
				.timeout(Duration.ofSeconds(this.defaultDeploymentProperties.getApiTimeout()))
				.doOnSuccess(v -> logger.info("Successfully undeployed app {}", appName))
				.doOnError(logError(String.format("Failed to undeploy app %s", appName)))
			.then(Mono.just(UndeployApplicationResponse.builder()
				.name(appName)
				.build()));
	}

	private Mono<Void> requestDeleteApplication(String name) {
		return this.operations.applications()
			.delete(DeleteApplicationRequest.builder()
				.deleteRoutes(defaultDeploymentProperties.isDeleteRoutes())
				.name(name)
				.build());
	}

	private Mono<Void> requestDeleteApplicationInSpace(String name, String space) {
		return createSpaceApplications(space)
			.delete(DeleteApplicationRequest.builder()
				.deleteRoutes(defaultDeploymentProperties.isDeleteRoutes())
				.name(name)
				.build());
	}

	private DefaultApplications createSpaceApplications(String space) {
		return new DefaultApplications(
			((DefaultCloudFoundryOperations) this.operations).getCloudFoundryClientPublisher(),
			((DefaultCloudFoundryOperations) this.operations).getDopplerClientPublisher(),
			this.operations.spaces().get(GetSpaceRequest.builder().name(space).build()).map(SpaceDetail::getId));
	}

	private DefaultSpaces createSpaceOperations() {
		return new DefaultSpaces(
			((DefaultCloudFoundryOperations) this.operations).getCloudFoundryClientPublisher() ,
			getOrganizationIdPublisher(),
			Mono.just(this.defaultDeploymentProperties.getUsername()));
	}

	private Map<String, String> getEnvironmentVariables(Map<String, String> environment) {
		Map<String, String> envVariables = new HashMap<>(getApplicationEnvironment(environment));

		String javaOpts = javaOpts(environment);
		if (StringUtils.hasText(javaOpts)) {
			envVariables.put("JAVA_OPTS", javaOpts(environment));
		}

		String group = environment.get(DeploymentProperties.GROUP_PROPERTY_KEY);
		if (StringUtils.hasText(group)) {
			envVariables.put("SPRING_CLOUD_APPLICATION_GROUP", group);
		}

		envVariables.put("SPRING_CLOUD_APPLICATION_GUID", "${vcap.application.name}:${vcap.application.instance_index}");
		envVariables.put("SPRING_APPLICATION_INDEX", "${vcap.application.instance_index}");

		return envVariables;
	}

	private Map<String, String> getApplicationEnvironment(Map<String, String> environment) {
		Map<String, String> applicationEnvironment = getSanitizedApplicationEnvironment(environment);

		if (!useSpringApplicationJson(environment)) {
			return applicationEnvironment;
		}

		try {
			return Collections.singletonMap("SPRING_APPLICATION_JSON",
				OBJECT_MAPPER.writeValueAsString(applicationEnvironment));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Error writing environment to SPRING_APPLICATION_JSON", e);
		}
	}

	private Map<String, String> getSanitizedApplicationEnvironment(Map<String, String> environment) {
		Map<String, String> applicationProperties = new HashMap<>(environment);

		// Remove server.port as CF assigns a port for us, and we don't want to override that
		Optional.ofNullable(applicationProperties.remove("server.port"))
			.ifPresent(port -> logger.warn("Ignoring 'server.port={}', " +
				"as Cloud Foundry will assign a local dynamic port. " +
				"Route to the app will use port 80.", port));

		return applicationProperties;
	}

	private boolean useSpringApplicationJson(Map<String, String> environment) {
		return Optional.ofNullable(environment.get(CloudFoundryDeploymentProperties.USE_SPRING_APPLICATION_JSON_KEY))
			.map(Boolean::valueOf)
			.orElse(this.defaultDeploymentProperties.isUseSpringApplicationJson());
	}

	private String domain(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.DOMAIN_PROPERTY))
			.orElse(this.defaultDeploymentProperties.getDomain());
	}

	private ApplicationHealthCheck healthCheck(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.HEALTHCHECK_PROPERTY_KEY))
			.map(this::toApplicationHealthCheck)
			.orElse(this.defaultDeploymentProperties.getHealthCheck());
	}

	private ApplicationHealthCheck toApplicationHealthCheck(String raw) {
		try {
			return ApplicationHealthCheck.from(raw);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(String.format("Unsupported health-check value '%s'. Available values are %s", raw,
				StringUtils.arrayToCommaDelimitedString(ApplicationHealthCheck.values())), e);
		}
	}

	private String healthCheckEndpoint(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.HEALTHCHECK_HTTP_ENDPOINT_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getHealthCheckHttpEndpoint());
	}

	private Integer healthCheckTimeout(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.HEALTHCHECK_TIMEOUT_PROPERTY_KEY))
			.map(Integer::parseInt)
			.orElse(this.defaultDeploymentProperties.getHealthCheckTimeout());
	}

	private Integer instances(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.COUNT_PROPERTY_KEY))
			.map(Integer::parseInt)
			.orElse(this.defaultDeploymentProperties.getInstances());
	}

	private String host(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.HOST_PROPERTY))
			.orElse(this.defaultDeploymentProperties.getHost());
	}

	private String routePath(Map<String, String> properties) {
		String routePath = properties.get(CloudFoundryDeploymentProperties.ROUTE_PATH_PROPERTY);
		if (StringUtils.hasText(routePath) && routePath.charAt(0) != '/') {
			throw new IllegalArgumentException(
					"Cloud Foundry routes must start with \"/\". Route passed = [" + routePath + "].");
		}
		return routePath;
	}

	private String route(Map<String, String> properties) {
		return properties.get(CloudFoundryDeploymentProperties.ROUTE_PROPERTY);
	}

	private Set<String> routes(Map<String, String> properties) {
		Set<String> routes = new HashSet<>();
		routes.addAll(this.defaultDeploymentProperties.getRoutes());
		routes.addAll(StringUtils.commaDelimitedListToSet(properties.get(CloudFoundryDeploymentProperties.ROUTES_PROPERTY)));
		return routes;
	}

	private Boolean toggleNoRoute(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.NO_ROUTE_PROPERTY))
				.map(Boolean::valueOf)
				.orElse(null);
	}

	private Integer memory(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.MEMORY_PROPERTY_KEY))
			.map(ByteSizeUtils::parseToMebibytes)
			.orElse(ByteSizeUtils.parseToMebibytes(defaultDeploymentProperties.getMemory()));
	}

	private Integer diskQuota(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.DISK_PROPERTY_KEY))
			.map(ByteSizeUtils::parseToMebibytes)
			.orElse(ByteSizeUtils.parseToMebibytes(defaultDeploymentProperties.getDisk()));
	}

	private String buildpack(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.BUILDPACK_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getBuildpack());
	}

	private String javaOpts(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.JAVA_OPTS_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getJavaOpts());
	}

	private Predicate<Throwable> isNotFoundError() {
		return t -> t instanceof AbstractCloudFoundryException &&
			((AbstractCloudFoundryException) t).getStatusCode() == HttpStatus.NOT_FOUND.value();
	}

	/**
	 * Return a Docker image identifier if the application Resource is for a Docker image, or {@literal null} otherwise.
	 *
	 * @see #getApplication(Resource)
	 */
	private String getDockerImage(Resource resource) {
		try {
			String uri = resource.getURI().toString();
			if (uri.startsWith("docker:")) {
				return uri.substring("docker:".length());
			} else {
				return null;
			}
		} catch (IOException e) {
			throw Exceptions.propagate(e);
		}
	}

	private Resource getAppResource(DeployApplicationRequest request) {
		return resourceLoader.getResource(request.getPath());
	}

	/**
	 * Return a Path to the application Resource or {@literal null} if the request is for a Docker image.
	 *
	 * @see #getDockerImage(Resource)
	 * @param resource the resource representing the app bits
	 */
	private Path getApplication(Resource resource) {
		try {
			if (resource.getURI().toString().startsWith("docker:")) {
				return null;
			} else {
				return resource.getFile().toPath();
			}
		} catch (IOException e) {
			throw Exceptions.propagate(e);
		}
	}

	/**
	 * Return a function usable in {@literal doOnError} constructs that will unwrap unrecognized Cloud Foundry Exceptions
	 * and log the text payload.
	 */
	private Consumer<Throwable> logError(String msg) {
		return e -> {
			if (e instanceof UnknownCloudFoundryException) {
				logger.error(msg + "\nUnknownCloudFoundryException encountered, whose payload follows:\n"
					+ ((UnknownCloudFoundryException)e).getPayload(), e);
			} else {
				logger.error(msg, e);
			}
		};
	}
}