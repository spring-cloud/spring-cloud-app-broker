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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.AbstractCloudFoundryException;
import org.cloudfoundry.UnknownCloudFoundryException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.AssociateApplicationRouteRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationResponse;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesRequest;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstanceEntity;
import org.cloudfoundry.client.v2.spaces.AssociateSpaceDeveloperRequest;
import org.cloudfoundry.client.v2.spaces.CreateSpaceRequest;
import org.cloudfoundry.client.v2.spaces.DeleteSpaceRequest;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.applications.ListApplicationPackagesRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationPackagesResponse;
import org.cloudfoundry.client.v3.builds.BuildState;
import org.cloudfoundry.client.v3.builds.CreateBuildRequest;
import org.cloudfoundry.client.v3.builds.CreateBuildResponse;
import org.cloudfoundry.client.v3.builds.GetBuildRequest;
import org.cloudfoundry.client.v3.builds.GetBuildResponse;
import org.cloudfoundry.client.v3.deployments.CreateDeploymentRequest;
import org.cloudfoundry.client.v3.deployments.CreateDeploymentResponse;
import org.cloudfoundry.client.v3.deployments.DeploymentRelationships;
import org.cloudfoundry.client.v3.deployments.DeploymentState;
import org.cloudfoundry.client.v3.deployments.DeploymentStatusReason;
import org.cloudfoundry.client.v3.deployments.DeploymentStatusValue;
import org.cloudfoundry.client.v3.deployments.GetDeploymentRequest;
import org.cloudfoundry.client.v3.deployments.GetDeploymentResponse;
import org.cloudfoundry.client.v3.packages.CreatePackageRequest;
import org.cloudfoundry.client.v3.packages.CreatePackageResponse;
import org.cloudfoundry.client.v3.packages.GetPackageRequest;
import org.cloudfoundry.client.v3.packages.GetPackageResponse;
import org.cloudfoundry.client.v3.packages.Package;
import org.cloudfoundry.client.v3.packages.PackageRelationships;
import org.cloudfoundry.client.v3.packages.PackageResource;
import org.cloudfoundry.client.v3.packages.PackageState;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.UploadPackageRequest;
import org.cloudfoundry.client.v3.packages.UploadPackageResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.Docker;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.Route;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationInfoRequest;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.operations.spaces.GetSpaceRequest;
import org.cloudfoundry.operations.spaces.SpaceDetail;
import org.cloudfoundry.operations.useradmin.SetSpaceRoleRequest;
import org.cloudfoundry.operations.useradmin.SpaceRole;
import org.cloudfoundry.util.DelayUtils;
import org.cloudfoundry.util.PaginationUtils;
import org.cloudfoundry.util.ResourceUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.CreateServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.CreateServiceInstanceResponse;
import org.springframework.cloud.appbroker.deployer.DeleteBackingSpaceRequest;
import org.springframework.cloud.appbroker.deployer.DeleteBackingSpaceResponse;
import org.springframework.cloud.appbroker.deployer.DeleteServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.DeleteServiceInstanceResponse;
import org.springframework.cloud.appbroker.deployer.DeployApplicationRequest;
import org.springframework.cloud.appbroker.deployer.DeployApplicationResponse;
import org.springframework.cloud.appbroker.deployer.DeploymentProperties;
import org.springframework.cloud.appbroker.deployer.GetApplicationRequest;
import org.springframework.cloud.appbroker.deployer.GetApplicationResponse;
import org.springframework.cloud.appbroker.deployer.GetServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.GetServiceInstanceResponse;
import org.springframework.cloud.appbroker.deployer.UndeployApplicationRequest;
import org.springframework.cloud.appbroker.deployer.UndeployApplicationResponse;
import org.springframework.cloud.appbroker.deployer.UpdateApplicationRequest;
import org.springframework.cloud.appbroker.deployer.UpdateApplicationResponse;
import org.springframework.cloud.appbroker.deployer.UpdateServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.UpdateServiceInstanceResponse;
import org.springframework.cloud.appbroker.deployer.util.ByteSizeUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.ExcessiveClassLength"})
public class CloudFoundryAppDeployer implements AppDeployer, ResourceLoaderAware {

	private static final Duration DEFAULT_PLATFORM_OPERATION_DURATION = Duration.ofMinutes(10);

	private static final Logger LOG = LoggerFactory.getLogger(CloudFoundryAppDeployer.class);

	private static final String REQUEST_LOG_TEMPLATE = "request={}";

	private static final String RESPONSE_LOG_TEMPLATE = "response={}";

	private static final String ERROR_LOG_TEMPLATE = "error=%s";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final CloudFoundryDeploymentProperties defaultDeploymentProperties;

	private final CloudFoundryOperations operations;

	private final CloudFoundryClient client;

	private final CloudFoundryOperationsUtils operationsUtils;

	private final CloudFoundryTargetProperties targetProperties;

	private ResourceLoader resourceLoader;

	public CloudFoundryAppDeployer(CloudFoundryDeploymentProperties deploymentProperties,
		CloudFoundryOperations operations,
		CloudFoundryClient client,
		CloudFoundryOperationsUtils operationsUtils,
		CloudFoundryTargetProperties targetProperties,
		ResourceLoader resourceLoader) {
		this.defaultDeploymentProperties = deploymentProperties;
		this.operations = operations;
		this.client = client;
		this.operationsUtils = operationsUtils;
		this.targetProperties = targetProperties;
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public Mono<GetApplicationResponse> get(GetApplicationRequest request) {
		final String appName = request.getName();

		return operationsUtils.getOperations(request.getProperties())
			.flatMap(cfOperations -> cfOperations.applications()
				.get(org.cloudfoundry.operations.applications.GetApplicationRequest.builder()
					.name(appName)
					.build())
				.doOnRequest(l -> {
					LOG.info("Getting application. appName={}", appName);
					LOG.debug(REQUEST_LOG_TEMPLATE, request);
				})
				.doOnSuccess(response -> {
					LOG.info("Success getting application. appName={}", appName);
					LOG.debug(RESPONSE_LOG_TEMPLATE, response);
				})
				.doOnError(e -> LOG.error(String.format("Error getting application. appName=%s, " + ERROR_LOG_TEMPLATE,
					appName, e.getMessage()), e))
				.map(ApplicationDetail::getId)
				.flatMap(id -> client.applicationsV2().summary(SummaryApplicationRequest.builder()
					.applicationId(id)
					.build())))
			.flatMap(summary -> Flux.fromIterable(summary.getServices())
				.map(org.cloudfoundry.client.v2.serviceinstances.ServiceInstance::getName)
				.collectList()
				.map(services -> GetApplicationResponse.builder()
					.id(summary.getId())
					.name(summary.getName())
					.services(services)
					.environment(summary.getEnvironmentJsons())
					.build()))
			.doOnRequest(l -> {
				LOG.info("Getting application summary. appName={}", appName);
				LOG.debug(REQUEST_LOG_TEMPLATE, request);
			})
			.doOnSuccess(response -> {
				LOG.info("Success getting application summary. appName={}", appName);
				LOG.debug(RESPONSE_LOG_TEMPLATE, response);
			})
			.doOnError(e -> LOG.error(String.format("Error getting application summary. appName=%s, " +
				ERROR_LOG_TEMPLATE, appName, e.getMessage()), e));
	}

	@Override
	public Mono<DeployApplicationResponse> deploy(DeployApplicationRequest request) {
		String appName = request.getName();
		Resource appResource = getAppResource(request.getPath());
		Map<String, String> deploymentProperties = request.getProperties();

		if (LOG.isTraceEnabled()) {
			LOG.trace("Deploying application: request={}, resource={}",
				appName, appResource);
		}

		return pushApplication(request, deploymentProperties, appResource)
			.timeout(Duration.ofSeconds(this.defaultDeploymentProperties.getApiTimeout()))
			.doOnSuccess(item -> LOG.info("Successfully deployed {}", appName))
			.doOnError(e -> {
				if (httpStatusNotFoundPredicate().test(e)) {
				LOG.error(String.format("Unable to deploy application. It may have been destroyed before " +
						"start completed. " + ERROR_LOG_TEMPLATE, e.getMessage()), e);
				}
				else {
					logError(String.format("Error deploying application. appName=%s", appName)).accept(e);
				}
			})
			.thenReturn(DeployApplicationResponse.builder()
				.name(appName)
				.build());
	}

	@Override
	public Mono<UpdateApplicationResponse> preUpdate(UpdateApplicationRequest request) {
		final String appName = request.getName();

		return get(GetApplicationRequest.builder()
			.name(appName)
			.properties(request.getProperties())
			.build())
			.map(GetApplicationResponse::getId)
			.flatMap(applicationId -> updateApplication(request, applicationId))
			.thenReturn(UpdateApplicationResponse.builder().name(appName).build());
	}

	@Override
	public Mono<UpdateApplicationResponse> update(UpdateApplicationRequest request) {
		final String appName = request.getName();

		return get(GetApplicationRequest.builder()
			.name(appName)
			.properties(request.getProperties())
			.build())
			.flatMap(response -> bindNewServices(response, request.getServices(), request.getProperties()))
			.flatMap(applicationId -> {
				if (request.getProperties().containsKey("routes")) {
					return associateRoutes(applicationId, request.getProperties());
				}
				else {
					return associateHostName(applicationId, request.getProperties());
				}
			})
			.flatMap(applicationId -> updateApplication(request, applicationId))
			.flatMap(applicationId -> Mono.zip(Mono.just(applicationId),
				upgradeApplicationIfRequired(request, applicationId)))
			.flatMap(tuple2 -> {
				String appId = tuple2.getT1();
				String packageId = tuple2.getT2();
				return Mono.zip(Mono.just(appId), createBuildForPackage(packageId));
			})
			.flatMap(tuple2 -> {
				String appId = tuple2.getT1();
				String buildId = tuple2.getT2();
				return Mono.zip(Mono.just(appId), waitForBuildStaged(buildId));
			})
			.map(tuple2 -> tuple2.mapT2((t2) -> t2.getDroplet().getId()))
			.flatMap(tuple2 -> {
				String appId = tuple2.getT1();
				String dropletId = tuple2.getT2();
				return createDeployment(dropletId, appId);
			})
			.map(CreateDeploymentResponse::getId)
			.flatMap(this::waitForDeploymentDeployed)
			.doOnRequest(l -> {
				LOG.info("Updating application. appName={}", appName);
				LOG.debug(REQUEST_LOG_TEMPLATE, request);
			})
			.doOnSuccess(response -> {
				LOG.info("Success updating application. appName={}", appName);
				LOG.debug(RESPONSE_LOG_TEMPLATE, response);
			})
			.doOnError(e -> LOG.error(String.format("Error updating application. appName=%s", appName), e))
			.thenReturn(UpdateApplicationResponse.builder().name(appName).build());
	}

	private Mono<String> bindNewServices(GetApplicationResponse deployedApp, List<String> services,
		Map<String, String> properties) {
		String id = deployedApp.getId();
		List<String> boundServices = deployedApp.getServices();
		List<String> servicesToBind = new ArrayList<>(services);
		servicesToBind.removeAll(boundServices);

		if (servicesToBind.isEmpty()) {
			return Mono.just(id);
		}

		String appName = deployedApp.getName();
		return operationsUtils.getOperations(properties)
			.flatMapMany(cfOperations -> Flux.fromIterable(servicesToBind)
				.flatMap(service -> cfOperations.services().bind(BindServiceInstanceRequest.builder()
					.applicationName(appName)
					.serviceInstanceName(service)
					.build())
					.doOnRequest(
						l -> LOG.info("Binding application to service. appName={}, serviceName={}", appName, service))
					.doOnNext(v -> LOG.info("Success binding application to service. appName={}, service={}", appName,
						service))
					.doOnError(e -> LOG.error(String.format("Error binding application to service. appName=%s, " +
						"service=%s", appName, service), e))))
			.then()
			.thenReturn(id);
	}

	private Mono<String> associateHostName(String applicationId, Map<String, String> properties) {
		String domain = domain(properties);
		Set<String> domains = domains(properties);
		String host = host(properties);
		if (host == null && domain == null && domains.isEmpty()) {
			return Mono.just(applicationId);
		}

		return operationsUtils.getOperations(properties)
			.map(cfOperations -> cfOperations.domains().list())
			.flatMap(Flux::collectList)
			.map(allDomains -> Stream.concat(Stream.of(domain), domains.stream())
				.map(d -> getDomainId(d, allDomains))
				.collect(Collectors.toSet()))
			.zipWith(getSpaceId(properties))
			.flatMapMany(domainIdsWithSpaceId -> {
				Set<String> uniqueDomainIds = domainIdsWithSpaceId.getT1();
				String spaceId = domainIdsWithSpaceId.getT2();
				return Flux.fromIterable(uniqueDomainIds)
					.flatMap(domainId -> associateHostForDomain(applicationId, host, domainId, spaceId));
			})
			.then(Mono.just(applicationId));
	}

	private Mono<String> associateRoutes(String applicationId, Map<String, String> properties) {
		List<String[]> routes = Arrays.stream(properties.get("routes").split(","))
			.map(uri -> uri.split("\\.", 2))
			.collect(Collectors.toList());

		return Mono.just(routes)
			.zipWith(operationsUtils.getOperations(properties)
				.map(cfOperations -> cfOperations.domains().list())
				.flatMap(domains -> domains.collectMap(Domain::getName)))
			.zipWith(getSpaceId(properties))
			.flatMapMany(routesDomainsAndSpace -> {
				List<String[]> routesComponents = routesDomainsAndSpace.getT1().getT1();
				Map<String, Domain> domainsByName = routesDomainsAndSpace.getT1().getT2();
				String spaceId = routesDomainsAndSpace.getT2();

				return Flux.fromStream(routesComponents.stream()
					.map(route -> new String[] {route[0], domainsByName.get(route[1]).getId()}))
					.flatMap(
						hostAndDomainId -> associateHostForDomain(applicationId, hostAndDomainId[0], hostAndDomainId[1],
							spaceId));
			})
			.then(Mono.just(applicationId));
	}

	private Mono<Void> associateHostForDomain(String applicationId, String host, String domainId, String spaceId) {
		return client.routes()
			.create(org.cloudfoundry.client.v2.routes.CreateRouteRequest.builder()
				.domainId(domainId)
				.spaceId(spaceId)
				.host(host)
				.build())
			.map(response -> response.getMetadata().getId())
			.doOnError(error -> LOG.info("Host was already associated. host={}", host))
			.onErrorResume(e -> Mono.empty())
			.flatMap(routeId -> client.applicationsV2().associateRoute(AssociateApplicationRouteRequest.builder()
				.applicationId(applicationId)
				.routeId(routeId)
				.build()))
			.then();
	}

	private Mono<String> getSpaceId(Map<String, String> properties) {
		String space;
		if (properties.containsKey(DeploymentProperties.TARGET_PROPERTY_KEY)) {
			space = properties.get(DeploymentProperties.TARGET_PROPERTY_KEY);
		}
		else {
			space = targetProperties.getDefaultSpace();
		}
		return operations
			.spaces()
			.get(GetSpaceRequest
				.builder()
				.name(space)
				.build())
			.map(SpaceDetail::getId);
	}

	private String getDomainId(String domain, List<Domain> domains) {
		if (domain == null) {
			return getDefaultDomainId(domains);
		}

		return domains.stream()
			.filter(d -> d.getName().equals(domain))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Non existing domain"))
			.getId();
	}

	private String getDefaultDomainId(List<Domain> domains) {
		return domains.stream()
			.filter(d -> !"internal".equals(d.getType()))
			.findFirst().orElseThrow(RuntimeException::new)
			.getId();
	}

	private Mono<GetDeploymentResponse> waitForDeploymentDeployed(String deploymentId) {
		return this.client
			.deploymentsV3()
			.get(GetDeploymentRequest
				.builder()
				.deploymentId(deploymentId)
				.build())
			.filter(this::deploymentFinished)
			.repeatWhenEmpty(getExponentialBackOff(getDeploymentTimeout()))
			.doOnRequest(l -> LOG.debug("Waiting for deployment to complete. deploymentId={}", deploymentId))
			.doOnSuccess(response -> {
				LOG.info("Success waiting for deployment to complete. deploymentId={}", deploymentId);
				LOG.debug(RESPONSE_LOG_TEMPLATE, response);
			})
			.doOnError(e -> LOG.error(String.format("Error waiting for deployment to complete. deploymentId=%s, " +
				ERROR_LOG_TEMPLATE, deploymentId, e.getMessage()), e));
	}

	@SuppressWarnings("deprecation")
	private boolean deploymentFinished(GetDeploymentResponse p) {
		if (p.getState() != null) {
			return p.getState().equals(DeploymentState.DEPLOYED);
		}

		return p.getStatus().getValue().equals(DeploymentStatusValue.FINALIZED)
			&& p.getStatus().getReason().equals(DeploymentStatusReason.DEPLOYED);
	}

	private Mono<CreateDeploymentResponse> createDeployment(String dropletId, String applicationId) {
		return this.client
			.deploymentsV3()
			.create(CreateDeploymentRequest
				.builder()
				.droplet(Relationship
					.builder()
					.id(dropletId).build()).relationships(DeploymentRelationships
					.builder()
					.app(ToOneRelationship
						.builder()
						.data(Relationship.builder().id(applicationId).build())
						.build()
					).build())
				.build())
			.doOnRequest(l -> LOG.debug("Creating deployment for application. applicationId={}", applicationId))
			.doOnSuccess(response -> {
				LOG.info("Success creating deployment for application. applicationId={}", applicationId);
				LOG.debug(RESPONSE_LOG_TEMPLATE, response);
			})
			.doOnError(e -> LOG.error(String.format("Error creating deployment for application. applicationId=%s, " +
				ERROR_LOG_TEMPLATE, applicationId, e.getMessage()), e));
	}

	private Mono<GetBuildResponse> waitForBuildStaged(String buildId) {
		return this.client.builds().get(GetBuildRequest.builder()
			.buildId(buildId)
			.build())
			.filter(p -> p.getState().equals(BuildState.STAGED))
			.repeatWhenEmpty(getExponentialBackOff(getStagingTimeout()))
			.doOnRequest(l -> LOG.debug("Waiting for build to stage. buildId={}", buildId))
			.doOnSuccess(response -> {
				LOG.info("Success waiting for build to stage. buildId={}", buildId);
				LOG.debug(RESPONSE_LOG_TEMPLATE, response);
			})
			.doOnError(e -> LOG.error(String.format("Error waiting for build to stage. buildId=%s, " +
				ERROR_LOG_TEMPLATE, buildId, e.getMessage()), e));
	}

	private Mono<String> createBuildForPackage(String packageId) {
		return this.client
			.builds()
			.create(CreateBuildRequest
				.builder()
				.getPackage(Relationship.builder().id(packageId).build())
				.build())
			.map(CreateBuildResponse::getId)
			.doOnRequest(l -> LOG.debug("Creating build for package. packageId={}", packageId))
			.doOnSuccess(response -> {
				LOG.info("Success creating build for package. packageId={}", packageId);
				LOG.debug(RESPONSE_LOG_TEMPLATE, response);
			})
			.doOnError(e -> LOG.error(String.format("Error creating build package. packageId=%s, " +
				ERROR_LOG_TEMPLATE, packageId, e.getMessage()), e));
	}

	private Mono<GetPackageResponse> waitForPackageReady(String packageId) {
		return this.client
			.packages()
			.get(GetPackageRequest.builder().packageId(packageId).build())
			.filter(p -> p.getState().equals(PackageState.READY))
			.repeatWhenEmpty(getExponentialBackOff(DEFAULT_PLATFORM_OPERATION_DURATION))
			.doOnRequest(l -> LOG.debug("Waiting for package ready. packageId={}", packageId))
			.doOnSuccess(response -> {
				LOG.info("Success waiting for package ready. packageId={}", packageId);
				LOG.debug(RESPONSE_LOG_TEMPLATE, response);
			})
			.doOnError(e -> LOG.error(String.format("Error waiting for package ready. packageId=%s, " +
				ERROR_LOG_TEMPLATE, packageId, e.getMessage()), e));
	}

	private Mono<UploadPackageResponse> uploadPackage(UpdateApplicationRequest request, String packageId) {
		try {
			return this.client
				.packages()
				.upload(UploadPackageRequest
					.builder()
					.packageId(packageId)
					.bits(Paths.get(getAppResource(request.getPath()).getURI()))
					.build())
				.doOnRequest(l -> {
					LOG.info("Uploading package. packageId={}", packageId);
					LOG.debug(REQUEST_LOG_TEMPLATE, request);
				})
				.doOnSuccess(response -> {
					LOG.info("Success uploading package. packageId={}", packageId);
					LOG.debug(RESPONSE_LOG_TEMPLATE, response);
				})
				.doOnError(e -> LOG.error(String.format("Error uploading package. packageId=%s, " + ERROR_LOG_TEMPLATE,
					packageId, e.getMessage()), e));
		}
		catch (IOException e) {
			throw Exceptions.propagate(e);
		}
	}

	private Mono<String> upgradeApplicationIfRequired(UpdateApplicationRequest request, String applicationId) {
		if (request.getProperties().containsKey("upgrade")) {
			return createPackageForApplication(applicationId)
				.map(CreatePackageResponse::getId)
				.flatMap(packageId -> uploadPackage(request, packageId))
				.map(UploadPackageResponse::getId)
				.flatMap(packageId -> waitForPackageReady(packageId)
					.map(Package::getId));
		}

		return getPackageForApplication(applicationId);
	}

	private Mono<String> updateApplication(UpdateApplicationRequest request, String applicationId) {
		final Map<String, Object> environmentVariables = getApplicationEnvironment(request.getProperties(),
			request.getEnvironment(), request.getServiceInstanceId());
		Map<String, String> properties = request.getProperties();

		return updateStackIfPresent(request, applicationId)
			.then(this.client.applicationsV2()
				.update(org.cloudfoundry.client.v2.applications.UpdateApplicationRequest.builder()
					.applicationId(applicationId)
					.instances(instances(properties))
					.diskQuota(diskQuota(properties))
					.memory(memory(properties))
					.putAllEnvironmentJsons(environmentVariables)
					.build())
				.doOnRequest(l -> LOG.debug("Updating environment. applicationId={}", applicationId))
				.doOnSuccess(response -> {
					LOG.info("Success updating environment. applicationId={}", applicationId);
					LOG.debug(RESPONSE_LOG_TEMPLATE, response);
				})
				.doOnError(e -> LOG.error(String.format("Error updating environment. applicationId=%s, " +
					ERROR_LOG_TEMPLATE, applicationId, e.getMessage()), e)))
			.thenReturn(applicationId);
	}

	private Mono<String> updateStackIfPresent(UpdateApplicationRequest request, String applicationId) {
		if (!request.getProperties().containsKey("upgrade") ||
			!StringUtils.hasText(this.defaultDeploymentProperties.getStack())) {
			return Mono.just(applicationId);
		}
		String stackName = this.defaultDeploymentProperties.getStack();
		return this.client.applicationsV3().
			update(org.cloudfoundry.client.v3.applications.UpdateApplicationRequest.builder()
				.applicationId(applicationId)
				.lifecycle(Lifecycle.builder()
					.type(LifecycleType.BUILDPACK)
					.data(BuildpackData.builder()
						.stack(stackName)
						.build())
					.build())
				.build())
			.thenReturn(applicationId);
	}

	private Mono<String> getPackageForApplication(String applicationId) {
		return this.client
			.applicationsV3()
			.listPackages(ListApplicationPackagesRequest
				.builder()
				.applicationId(applicationId)
				.state(PackageState.READY)
				.build())
			.doOnRequest(l -> LOG.debug("Getting application package. applicationId={}", applicationId))
			.doOnSuccess(response -> {
				LOG.info("Success getting application package. applicationId={}", applicationId);
				LOG.debug(RESPONSE_LOG_TEMPLATE, response);
			})
			.doOnError(e -> LOG.error(String.format("Error getting application package. applicationId=%s, " +
				ERROR_LOG_TEMPLATE, applicationId, e.getMessage()), e))
			.map(ListApplicationPackagesResponse::getResources)
			.map(this::getLastUpdatedPackageId);
	}

	private String getLastUpdatedPackageId(List<PackageResource> packageResources) {
		return packageResources
			.stream()
			.min((h1, h2) -> Instant.parse(h2.getUpdatedAt()).compareTo(Instant.parse(h1.getUpdatedAt())))
			.orElse(packageResources.get(0))
			.getId();
	}

	private Mono<CreatePackageResponse> createPackageForApplication(String applicationId) {
		return this.client
			.packages()
			.create(CreatePackageRequest
				.builder()
				.relationships(PackageRelationships
					.builder()
					.application(ToOneRelationship
						.builder()
						.data(Relationship
							.builder()
							.id(applicationId)
							.build())
						.build())
					.build())
				.type(PackageType.BITS)
				.build())
			.doOnRequest(l -> LOG.debug("Creating package. applicationId={}", applicationId))
			.doOnSuccess(response -> {
				LOG.info("Success creating package. applicationId={}", applicationId);
				LOG.debug(RESPONSE_LOG_TEMPLATE, response);
			})
			.doOnError(e -> LOG.error(String.format("Error creating package. applicationId=%s, " + ERROR_LOG_TEMPLATE,
				applicationId, e.getMessage()), e));
	}

	private Duration getStagingTimeout() {
		if (targetProperties.getStagingTimeout() == null) {
			return DEFAULT_PLATFORM_OPERATION_DURATION;
		}
		return Duration.ofMinutes(targetProperties.getStagingTimeout());
	}

	private Duration getDeploymentTimeout() {
		if (targetProperties.getDeploymentTimeout() == null) {
			return DEFAULT_PLATFORM_OPERATION_DURATION;
		}
		return Duration.ofMinutes(targetProperties.getDeploymentTimeout());
	}

	private Function<Flux<Long>, Publisher<?>> getExponentialBackOff(Duration timeout) {
		return DelayUtils.exponentialBackOff(Duration.ofSeconds(2), Duration.ofMinutes(5), timeout);
	}

	private Mono<Void> pushApplication(DeployApplicationRequest request, Map<String, String> deploymentProperties,
		Resource appResource) {
		ApplicationManifest manifest = buildAppManifest(request, deploymentProperties, appResource);

		LOG.debug("Pushing app manifest. manifest={}", manifest.toString());

		PushApplicationManifestRequest applicationManifestRequest =
			PushApplicationManifestRequest.builder()
				.manifest(manifest)
				.stagingTimeout(this.defaultDeploymentProperties.getStagingTimeout())
				.startupTimeout(this.defaultDeploymentProperties.getStartupTimeout())
				.noStart(!start(deploymentProperties))
				.build();

		Mono<Void> requestPushApplication;
		if (deploymentProperties.containsKey(DeploymentProperties.TARGET_PROPERTY_KEY)) {
			String space = deploymentProperties.get(DeploymentProperties.TARGET_PROPERTY_KEY);
			requestPushApplication = pushManifestInSpace(applicationManifestRequest, space);
		}
		else {
			requestPushApplication = pushManifest(applicationManifestRequest);
		}

		return requestPushApplication
			.doOnSuccess(v -> LOG.info("Success pushing app manifest. appName={}", request.getName()))
			.doOnError(e -> LOG.error(String.format("Error pushing app manifest. appName=%s, " + ERROR_LOG_TEMPLATE,
				request.getName(), e.getMessage()), e));
	}

	private ApplicationManifest buildAppManifest(DeployApplicationRequest request,
		Map<String, String> deploymentProperties,
		Resource appResource) {
		ApplicationManifest.Builder manifest = ApplicationManifest.builder()
			.name(request.getName())
			.path(getApplication(appResource))
			.environmentVariables(getEnvironmentVariables(deploymentProperties,
				request.getEnvironment(), request.getServiceInstanceId()))
			.services(request.getServices())
			.instances(instances(deploymentProperties))
			.memory(memory(deploymentProperties))
			.stack(stack(deploymentProperties))
			.disk(diskQuota(deploymentProperties))
			.healthCheckType(healthCheck(deploymentProperties))
			.healthCheckHttpEndpoint(healthCheckEndpoint(deploymentProperties))
			.timeout(healthCheckTimeout(deploymentProperties))
			.noRoute(toggleNoRoute(deploymentProperties));

		Optional.ofNullable(routePath(deploymentProperties)).ifPresent(manifest::routePath);

		if (routes(deploymentProperties).isEmpty()) {
			Optional.ofNullable(host(deploymentProperties)).ifPresent(manifest::host);
			Optional.ofNullable(domain(deploymentProperties)).ifPresent(manifest::domain);
			if (!domains(deploymentProperties).isEmpty()) {
				domains(deploymentProperties).forEach(manifest::domain);
			}
		}
		else {
			Set<Route> routes = routes(deploymentProperties).stream()
				.map(r -> Route.builder().route(r).build())
				.collect(Collectors.toSet());
			manifest.routes(routes);
		}

		if (getDockerImage(appResource) == null) {
			manifest.buildpack(buildpack(deploymentProperties));
			String buildpacks = buildpacks(deploymentProperties);
			if (StringUtils.hasText(buildpacks)) {
				manifest.buildpacks(Arrays.asList(buildpacks.split(",")));
			}
		}
		else {
			manifest.docker(Docker.builder().image(getDockerImage(appResource)).build());
		}

		return manifest.build();
	}

	private Mono<Void> pushManifest(PushApplicationManifestRequest request) {
		return this.operations.applications()
			.pushManifest(request);
	}

	private Mono<Void> pushManifestInSpace(PushApplicationManifestRequest request, String spaceName) {
		return createSpace(spaceName)
			.then(operationsUtils.getOperationsForSpace(spaceName))
			.flatMap(cfOperations -> cfOperations.applications().pushManifest(request));
	}

	private Mono<String> createSpace(String spaceName) {
		return getSpaceId(spaceName)
			.switchIfEmpty(Mono.justOrEmpty(targetProperties.getDefaultOrg())
				.flatMap(orgName -> getOrganizationId(orgName)
					.flatMap(orgId -> client.spaces().create(CreateSpaceRequest.builder()
						.organizationId(orgId)
						.name(spaceName)
						.build())
						.doOnSuccess(response -> {
							LOG.info("Success creating space. spaceName={}", spaceName);
							LOG.debug(RESPONSE_LOG_TEMPLATE, response);
						})
						.doOnError(e -> LOG.error(String.format("Error creating space. spaceName=%s, " +
							ERROR_LOG_TEMPLATE, spaceName, e.getMessage()), e))
						.onErrorResume(e -> Mono.empty())
						.map(response -> response.getMetadata().getId())
						.flatMap(spaceId -> addSpaceDeveloperRoleForCurrentUser(orgName, spaceName, spaceId)
							.thenReturn(spaceId)))));
	}

	private Mono<Void> addSpaceDeveloperRoleForCurrentUser(String orgName, String spaceName, String spaceId) {
		return Mono.defer(() -> {
			if (StringUtils.hasText(targetProperties.getClientId())) {
				return client.spaces().associateDeveloper(AssociateSpaceDeveloperRequest.builder()
					.spaceId(spaceId)
					.developerId(targetProperties.getClientId())
					.build())
					.doOnSuccess(response -> {
						LOG.info("Setting space developer role. spaceName={}", spaceName);
						LOG.debug(RESPONSE_LOG_TEMPLATE, response);
					})
					.doOnError(e -> LOG.error(String.format("Error setting space developer role. spaceName=%s, " +
						ERROR_LOG_TEMPLATE, spaceName, e.getMessage()), e))
					.then();
			}
			else if (StringUtils.hasText(targetProperties.getUsername())) {
				return operations.userAdmin().setSpaceRole(SetSpaceRoleRequest.builder()
					.spaceRole(SpaceRole.DEVELOPER)
					.organizationName(orgName)
					.spaceName(spaceName)
					.username(targetProperties.getUsername())
					.build())
					.doOnSuccess(v -> LOG.info("Seting space developer role. spaceName={}", spaceName))
					.doOnError(e -> LOG.error(String.format("Error setting space developer role. spaceName=%s, " +
						ERROR_LOG_TEMPLATE, spaceName, e.getMessage()), e));
			}
			return Mono.empty();
		});
	}

	private Mono<String> getOrganizationId(String orgName) {
		return operations.organizations().get(OrganizationInfoRequest.builder()
			.name(orgName)
			.build())
			.map(OrganizationDetail::getId);
	}

	@Override
	public Mono<UndeployApplicationResponse> undeploy(UndeployApplicationRequest request) {
		LOG.trace("Undeploying application. request={}", request);

		String appName = request.getName();
		Map<String, String> deploymentProperties = request.getProperties();

		Mono<Void> requestDeleteApplication;
		if (deploymentProperties.containsKey(DeploymentProperties.TARGET_PROPERTY_KEY)) {
			String space = deploymentProperties.get(DeploymentProperties.TARGET_PROPERTY_KEY);
			requestDeleteApplication = deleteApplicationInSpace(appName, space);
		}
		else {
			requestDeleteApplication = deleteApplication(appName);
		}

		return requestDeleteApplication
			.timeout(Duration.ofSeconds(this.defaultDeploymentProperties.getApiTimeout()))
			.doOnSuccess(v -> LOG.info("Success undeploying application. appName={}", appName))
			.doOnError(logError(String.format("Error undeploying application. appName=%s", appName)))
			.then(Mono.just(UndeployApplicationResponse.builder()
				.name(appName)
				.build()));
	}

	private Mono<Void> deleteApplication(String name) {
		return this.operations.applications()
			.delete(DeleteApplicationRequest.builder()
				.deleteRoutes(this.defaultDeploymentProperties.isDeleteRoutes())
				.name(name)
				.build());
	}

	private Mono<Void> deleteApplicationInSpace(String name, String spaceName) {
		return getSpaceId(spaceName)
			.doOnError(e -> LOG.error(String.format("Unable to get space name. spaceName=%s, " + ERROR_LOG_TEMPLATE,
				spaceName, e.getMessage()), e))
			.then(operationsUtils.getOperationsForSpace(spaceName))
			.flatMap(cfOperations -> cfOperations.applications().delete(DeleteApplicationRequest.builder()
				.deleteRoutes(this.defaultDeploymentProperties.isDeleteRoutes())
				.name(name)
				.build())
				.doOnError(e -> LOG.error(String.format("Error deleting application. appName=%s, " + ERROR_LOG_TEMPLATE,
					name, e.getMessage()), e)))
			.onErrorResume(e -> Mono.empty());
	}

	@Override
	public Mono<DeleteBackingSpaceResponse> deleteBackingSpace(DeleteBackingSpaceRequest request) {
		String spaceName = request.getName();
		return getSpaceId(spaceName)
			.doOnError(e -> LOG.error(String.format("Unable to get space name. spaceName=%s, " + ERROR_LOG_TEMPLATE,
				spaceName, e.getMessage()), e))
			.onErrorResume(e -> Mono.empty())
			.flatMap(spaceId -> this.client.spaces()
				.delete(DeleteSpaceRequest.builder()
					.spaceId(spaceId)
					.recursive(true)
					.build()))
			.doOnError(e -> LOG.error(String.format("Error deleting space. spaceName=%s, " + ERROR_LOG_TEMPLATE,
				spaceName, e.getMessage()), e))
			.thenReturn(DeleteBackingSpaceResponse.builder().name(spaceName).build());
	}

	private Mono<String> getSpaceId(String spaceName) {
		return Mono.justOrEmpty(targetProperties.getDefaultOrg())
			.flatMap(orgName -> getOrganizationId(orgName)
				.flatMap(orgId -> PaginationUtils.requestClientV2Resources(page -> client.organizations()
					.listSpaces(ListOrganizationSpacesRequest.builder()
						.name(spaceName)
						.organizationId(orgId)
						.page(page)
						.build()))
					.filter(resource -> resource.getEntity().getName().equals(spaceName))
					.map(resource -> resource.getMetadata().getId())
					.next()));
	}

	private Map<String, Object> getEnvironmentVariables(Map<String, String> properties,
		Map<String, Object> environment,
		String serviceInstanceId) {
		Map<String, Object> envVariables = getApplicationEnvironment(properties, environment, serviceInstanceId);

		String javaOpts = javaOpts(properties);
		if (StringUtils.hasText(javaOpts)) {
			envVariables.put("JAVA_OPTS", javaOpts);
		}

		return envVariables;
	}

	private Map<String, Object> getApplicationEnvironment(Map<String, String> properties,
		Map<String, Object> environment,
		String serviceInstanceId) {
		Map<String, Object> applicationEnvironment = sanitizeApplicationEnvironment(environment);

		if (serviceInstanceId != null) {
			applicationEnvironment.put("spring.cloud.appbroker.service-instance-id", serviceInstanceId);
		}

		if (!applicationEnvironment.isEmpty() && useSpringApplicationJson(properties)) {
			try {
				String jsonEnvironment = OBJECT_MAPPER.writeValueAsString(applicationEnvironment);
				applicationEnvironment = new HashMap<>(1);
				applicationEnvironment.put("SPRING_APPLICATION_JSON", jsonEnvironment);
			}
			catch (JsonProcessingException e) {
				throw new IllegalArgumentException("Error writing environment to SPRING_APPLICATION_JSON", e);
			}
		}

		return applicationEnvironment;
	}

	private Map<String, Object> sanitizeApplicationEnvironment(Map<String, Object> environment) {
		Map<String, Object> applicationEnvironment = new HashMap<>(environment);

		// Remove server.port as CF assigns a port for us, and we don't want to override that
		Optional.ofNullable(applicationEnvironment.remove("server.port"))
			.ifPresent(port -> LOG.warn("Ignoring 'server.port={}', " +
				"as Cloud Foundry will assign a local dynamic port. " +
				"Route to the app will use port 80.", port));

		return applicationEnvironment;
	}

	private boolean useSpringApplicationJson(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.USE_SPRING_APPLICATION_JSON_KEY))
			.map(Boolean::valueOf)
			.orElse(this.defaultDeploymentProperties.isUseSpringApplicationJson());
	}

	private String domain(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.DOMAIN_PROPERTY))
			.orElse(this.defaultDeploymentProperties.getDomain());
	}

	private Set<String> domains(Map<String, String> properties) {
		Set<String> domains = new HashSet<>();
		domains.addAll(this.defaultDeploymentProperties.getDomains());
		domains.addAll(
			StringUtils.commaDelimitedListToSet(properties.get(CloudFoundryDeploymentProperties.DOMAINS_PROPERTY)));
		return domains;
	}

	private ApplicationHealthCheck healthCheck(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.HEALTHCHECK_PROPERTY_KEY))
			.map(this::toApplicationHealthCheck)
			.orElse(this.defaultDeploymentProperties.getHealthCheck());
	}

	private ApplicationHealthCheck toApplicationHealthCheck(String raw) {
		try {
			return ApplicationHealthCheck.from(raw);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
				String.format("Unsupported health-check value '%s'. Available values are %s", raw,
					StringUtils.arrayToCommaDelimitedString(ApplicationHealthCheck.values())), e);
		}
	}

	private String healthCheckEndpoint(Map<String, String> properties) {
		return Optional
			.ofNullable(properties.get(CloudFoundryDeploymentProperties.HEALTHCHECK_HTTP_ENDPOINT_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getHealthCheckHttpEndpoint());
	}

	private Integer healthCheckTimeout(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.HEALTHCHECK_TIMEOUT_PROPERTY_KEY))
			.map(Integer::parseInt)
			.orElse(this.defaultDeploymentProperties.getHealthCheckTimeout());
	}

	private Duration apiPollingTimeout(Map<String, String> properties) {
		return Duration.ofSeconds(
			Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.API_POLLING_TIMEOUT_PROPERTY_KEY))
				.map(Long::parseLong)
				.orElse(this.defaultDeploymentProperties.getApiPollingTimeout()));
	}

	private Integer instances(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.COUNT_PROPERTY_KEY))
			.map(Integer::parseInt)
			.orElse(this.defaultDeploymentProperties.getCount());
	}

	private String host(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(DeploymentProperties.HOST_PROPERTY_KEY))
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

	private Set<String> routes(Map<String, String> properties) {
		Set<String> routes = new HashSet<>();
		routes.addAll(this.defaultDeploymentProperties.getRoutes());
		routes.addAll(
			StringUtils.commaDelimitedListToSet(properties.get(CloudFoundryDeploymentProperties.ROUTES_PROPERTY)));
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

	private String buildpacks(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.BUILDPACKS_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getBuildpacks());
	}

	private String stack(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.STACK_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getStack());
	}

	private String javaOpts(Map<String, String> properties) {
		return Optional.ofNullable(properties.get(CloudFoundryDeploymentProperties.JAVA_OPTS_PROPERTY_KEY))
			.orElse(this.defaultDeploymentProperties.getJavaOpts());
	}

	private Boolean start(Map<String, String> deploymentProperties) {
		return Optional.ofNullable(deploymentProperties.get(DeploymentProperties.START_PROPERTY_KEY))
			.map(Boolean::valueOf)
			.orElse(true);
	}

	private Predicate<Throwable> httpStatusNotFoundPredicate() {
		return t -> t instanceof AbstractCloudFoundryException &&
			((AbstractCloudFoundryException) t).getStatusCode() == HttpStatus.NOT_FOUND.value();
	}

	/**
	 * Return a Docker image identifier if the application Resource is for a Docker image, or {@literal null}
	 * otherwise.
	 *
	 * @see #getApplication(Resource)
	 */
	private String getDockerImage(Resource resource) {
		try {
			String uri = resource.getURI().toString();
			if (uri.startsWith("docker:")) {
				return uri.substring("docker:".length());
			}
			else {
				return null;
			}
		}
		catch (IOException e) {
			throw Exceptions.propagate(e);
		}
	}

	private Resource getAppResource(String path) {
		return resourceLoader.getResource(path);
	}

	/**
	 * Return a Path to the application Resource or {@literal null} if the request is for a Docker image.
	 *
	 * @param resource the resource representing the app bits
	 * @see #getDockerImage(Resource)
	 */
	private Path getApplication(Resource resource) {
		try {
			if (resource.getURI().toString().startsWith("docker:")) {
				return null;
			}
			else {
				return resource.getFile().toPath();
			}
		}
		catch (IOException e) {
			throw Exceptions.propagate(e);
		}
	}

	@Override
	public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
		return Mono.defer(() -> {
			if (StringUtils.hasText(request.getServiceInstanceId())) {
				return getServiceInstance(request.getServiceInstanceId())
					.flatMap(serviceInstanceEntity -> getSpace(serviceInstanceEntity.getSpaceId())
						.flatMap(spaceEntity -> getServiceInstance(serviceInstanceEntity.getName(), spaceEntity)));
			}
			else {
				return operationsUtils.getOperations(request.getProperties())
					.flatMap(cfOperations -> cfOperations.services()
						.getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
							.name(request.getName())
							.build()));
			}
		})
			.map(serviceInstance -> GetServiceInstanceResponse.builder()
				.name(serviceInstance.getName())
				.service(serviceInstance.getService())
				.plan(serviceInstance.getPlan())
				.build());
	}

	private Mono<ServiceInstance> getServiceInstance(String name, SpaceEntity spaceEntity) {
		return getOrganization(spaceEntity.getOrganizationId())
			.flatMap(organizationEntity ->
				operationsUtils.getOperationsForOrgAndSpace(organizationEntity.getName(), spaceEntity.getName())
					.flatMap(cfOperations -> cfOperations.services()
						.getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
							.name(name)
							.build())));
	}

	private Mono<ServiceInstanceEntity> getServiceInstance(String serviceInstanceId) {
		return client.serviceInstances()
			.get(org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceRequest.builder()
				.serviceInstanceId(serviceInstanceId)
				.build())
			.map(ResourceUtils::getEntity);
	}

	private Mono<SpaceEntity> getSpace(String spaceId) {
		return client.spaces().get(org.cloudfoundry.client.v2.spaces.GetSpaceRequest.builder()
			.spaceId(spaceId)
			.build())
			.map(ResourceUtils::getEntity);
	}

	private Mono<OrganizationEntity> getOrganization(String organizationId) {
		return client.organizations()
			.get(GetOrganizationRequest.builder().organizationId(organizationId).build())
			.map(GetOrganizationResponse::getEntity);
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		org.cloudfoundry.operations.services.CreateServiceInstanceRequest createServiceInstanceRequest =
			org.cloudfoundry.operations.services.CreateServiceInstanceRequest
				.builder()
				.serviceInstanceName(request.getServiceInstanceName())
				.serviceName(request.getName())
				.planName(request.getPlan())
				.parameters(request.getParameters())
				.completionTimeout(apiPollingTimeout(request.getProperties()))
				.build();

		Mono<CreateServiceInstanceResponse> createServiceInstanceResponseMono =
			Mono.just(CreateServiceInstanceResponse.builder()
				.name(request.getServiceInstanceName())
				.build());

		if (request.getProperties().containsKey(DeploymentProperties.TARGET_PROPERTY_KEY)) {
			return createSpace(request.getProperties().get(DeploymentProperties.TARGET_PROPERTY_KEY))
				.then(
					operationsUtils.getOperations(request.getProperties())
						.flatMap(cfOperations -> cfOperations.services()
							.createInstance(createServiceInstanceRequest)
							.then(createServiceInstanceResponseMono)));
		}
		else {
			return operations
				.services()
				.createInstance(createServiceInstanceRequest)
				.then(createServiceInstanceResponseMono);
		}
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		return operationsUtils.getOperations(request.getProperties())
			.flatMap(cfOperations -> rebindServiceInstanceIfNecessary(request, cfOperations)
				.then(updateServiceInstanceIfNecessary(request, cfOperations)));
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		String serviceInstanceName = request.getServiceInstanceName();
		Map<String, String> deploymentProperties = request.getProperties();

		Mono<Void> requestDeleteServiceInstance;
		if (deploymentProperties.containsKey(DeploymentProperties.TARGET_PROPERTY_KEY)) {
			requestDeleteServiceInstance = operationsUtils.getOperations(deploymentProperties)
				.flatMap(cfOperations -> unbindServiceInstance(serviceInstanceName, cfOperations)
					.then(deleteServiceInstance(serviceInstanceName, cfOperations, deploymentProperties)));
		}
		else {
			requestDeleteServiceInstance = unbindServiceInstance(serviceInstanceName, operations)
				.then(deleteServiceInstance(serviceInstanceName, operations, deploymentProperties));
		}

		return requestDeleteServiceInstance
			.doOnSuccess(v -> LOG.info("Success deleting service instance. serviceInstanceName={}",
				serviceInstanceName))
			.doOnError(logError(String.format("Error deleting service instance. serviceInstanceName=%s",
				serviceInstanceName)))
			.thenReturn(DeleteServiceInstanceResponse.builder()
				.name(serviceInstanceName)
				.build());
	}

	private Mono<Void> deleteServiceInstance(String serviceInstanceName,
		CloudFoundryOperations cloudFoundryOperations,
		Map<String, String> deploymentProperties) {
		return cloudFoundryOperations.services().deleteInstance(
			org.cloudfoundry.operations.services.DeleteServiceInstanceRequest.builder()
				.name(serviceInstanceName)
				.completionTimeout(apiPollingTimeout(deploymentProperties))
				.build())
			.doOnError(e -> LOG.error(String.format("Error deleting service instance. serviceInstanceName=%s, " +
				ERROR_LOG_TEMPLATE, serviceInstanceName, e.getMessage()), e))
			.onErrorResume(e -> Mono.empty());
	}

	private Mono<Void> unbindServiceInstance(String serviceInstanceName,
		CloudFoundryOperations cloudFoundryOperations) {
		return cloudFoundryOperations.services()
			.getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
				.name(serviceInstanceName)
				.build())
			.doOnError(e -> LOG.error(String.format("Error getting service instance. serviceInstanceName=%s, " +
				ERROR_LOG_TEMPLATE, serviceInstanceName, e.getMessage()), e))
			.onErrorResume(e -> Mono.empty())
			.map(ServiceInstance::getApplications)
			.flatMap(applications -> Flux.fromIterable(applications)
				.flatMap(application -> cloudFoundryOperations.services().unbind(
					UnbindServiceInstanceRequest.builder()
						.applicationName(application)
						.serviceInstanceName(serviceInstanceName)
						.build())
				)
				.doOnError(e -> LOG.error(String.format("Error unbinding service instance. serviceInstanceName=%s, " +
					ERROR_LOG_TEMPLATE, serviceInstanceName, e.getMessage()), e))
				.onErrorResume(e -> Mono.empty())
				.then(Mono.empty()));
	}

	private Mono<Void> rebindServiceInstance(String serviceInstanceName,
		CloudFoundryOperations cloudFoundryOperations) {
		return cloudFoundryOperations.services()
			.getInstance(org.cloudfoundry.operations.services.GetServiceInstanceRequest.builder()
				.name(serviceInstanceName)
				.build())
			.map(ServiceInstance::getApplications)
			.flatMap(applications -> Flux.fromIterable(applications)
				.flatMap(application -> cloudFoundryOperations.services().unbind(
					UnbindServiceInstanceRequest.builder()
						.applicationName(application)
						.serviceInstanceName(serviceInstanceName)
						.build())
					.then(cloudFoundryOperations.services().bind(
						BindServiceInstanceRequest.builder()
							.applicationName(application)
							.serviceInstanceName(serviceInstanceName)
							.build()))
				)
				.then(Mono.empty()));
	}

	private Mono<Void> rebindServiceInstanceIfNecessary(UpdateServiceInstanceRequest request,
		CloudFoundryOperations cloudFoundryOperations) {
		if (request.isRebindOnUpdate()) {
			return rebindServiceInstance(request.getServiceInstanceName(), cloudFoundryOperations);
		}
		return Mono.empty();
	}

	private Mono<UpdateServiceInstanceResponse> updateServiceInstanceIfNecessary(UpdateServiceInstanceRequest request,
		CloudFoundryOperations cloudFoundryOperations) {
		// service instances can be updated with a change to the plan, name, or parameters;
		// of these only parameter changes are supported, so don't update if the
		// backing service instance has no parameters
		if (request.getParameters() == null || request.getParameters().isEmpty()) {
			return Mono.empty();
		}

		final String serviceInstanceName = request.getServiceInstanceName();

		return cloudFoundryOperations.services().updateInstance(
			org.cloudfoundry.operations.services.UpdateServiceInstanceRequest.builder()
				.serviceInstanceName(serviceInstanceName)
				.completionTimeout(apiPollingTimeout(request.getProperties()))
				.parameters(request.getParameters())
				.build())
			.then(Mono.just(UpdateServiceInstanceResponse.builder()
				.name(serviceInstanceName)
				.build()));
	}

	/**
	 * Return a function usable in {@literal doOnError} constructs that will unwrap unrecognized Cloud Foundry
	 * Exceptions and log the text payload.
	 */
	private Consumer<Throwable> logError(String msg) {
		return e -> {
			if (e instanceof UnknownCloudFoundryException) {
				if (LOG.isErrorEnabled()) {
					LOG.error(msg + "\nUnknownCloudFoundryException encountered, whose payload follows:\n"
						+ ((UnknownCloudFoundryException) e).getPayload(), e);
				}
			}
			else {
				if (LOG.isErrorEnabled()) {
					LOG.error(msg, e);
				}
			}
		};
	}

}
