package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.Docker;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.appbroker.deployer.ReactiveAppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.cloudfoundry.AppNameGenerator;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppInstanceStatus;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.DOMAIN_PROPERTY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.HEALTHCHECK_HTTP_ENDPOINT_PROPERTY_KEY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.HEALTHCHECK_PROPERTY_KEY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.HEALTHCHECK_TIMEOUT_PROPERTY_KEY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.HOST_PROPERTY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.NO_ROUTE_PROPERTY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.ROUTES_PROPERTY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.ROUTE_PATH_PROPERTY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.ROUTE_PROPERTY;
import static org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties.USE_SPRING_APPLICATION_JSON_KEY;

public class CloudFoundryReactiveAppDeployer extends AbstractCloudFoundryReactiveAppDeployer implements ReactiveAppDeployer {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final Logger logger = LoggerFactory.getLogger(org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer.class);

	private final AppNameGenerator applicationNameGenerator;

	private final CloudFoundryOperations operations;

	public CloudFoundryReactiveAppDeployer(AppNameGenerator applicationNameGenerator,
										   CloudFoundryDeploymentProperties deploymentProperties,
										   CloudFoundryOperations operations,
										   RuntimeEnvironmentInfo runtimeEnvironmentInfo) {
		super(deploymentProperties, runtimeEnvironmentInfo);
		this.operations = operations;
		this.applicationNameGenerator = applicationNameGenerator;
	}

	@Override
	public Mono<String> deploy(AppDeploymentRequest request) {
		logger.trace("Entered deploy: Deploying AppDeploymentRequest: AppDefinition = {}, Resource = {}, Deployment Properties = {}",
				request.getDefinition(), request.getResource(), request.getDeploymentProperties());
		String deploymentId = deploymentId(request);

		logger.trace("deploy: Pushing application");
		return pushApplication(deploymentId, request)
				.timeout(Duration.ofSeconds(this.deploymentProperties.getApiTimeout()))
				.doOnSuccess(item -> logger.info("Successfully deployed {}", deploymentId))
				.doOnError(error -> {
					if (isNotFoundError().test(error)) {
						logger.warn("Unable to deploy application. It may have been destroyed before start completed: " + error.getMessage());
					}
					else {
						logError(String.format("Failed to deploy %s", deploymentId)).accept(error);
					}
				})
				.then(Mono.just(deploymentId));
	}

	private Mono<Void> pushApplication(String deploymentId, AppDeploymentRequest request) {
		ApplicationManifest manifest = buildAppManifest(deploymentId, request);

		logger.debug("Pushing manifest" + manifest.toString());

		return requestPushApplication(
				PushApplicationManifestRequest.builder()
						.manifest(manifest)
						.stagingTimeout(this.deploymentProperties.getStagingTimeout())
						.startupTimeout(this.deploymentProperties.getStartupTimeout())
						.build())
				.doOnSuccess(v -> logger.info("Done uploading bits for {}", deploymentId))
				.doOnError(e -> logger.error(String.format("Error creating app %s.  Exception Message %s", deploymentId, e.getMessage())));
	}

	private ApplicationManifest buildAppManifest(String deploymentId, AppDeploymentRequest request) {
		ApplicationManifest.Builder manifest = ApplicationManifest.builder()
				.path(getApplication(request)) // Only one of the two is non-null
				.disk(diskQuota(request))
				.environmentVariables(getEnvironmentVariables(deploymentId, request))
				.healthCheckType(healthCheck(request))
				.healthCheckHttpEndpoint(healthCheckEndpoint(request))
				.timeout(healthCheckTimeout(request))
				.instances(instances(request))
				.memory(memory(request))
				.name(deploymentId)
				.noRoute(toggleNoRoute(request))
				.services(servicesToBind(request));

		Optional.ofNullable(host(request)).ifPresent(manifest::host);
		Optional.ofNullable(domain(request)).ifPresent(manifest::domain);
		Optional.ofNullable(routePath(request)).ifPresent(manifest::routePath);

		if (route(request) != null) {
			manifest.route(Route.builder().route(route(request)).build());
		}

		if (! routes(request).isEmpty()){
			Set<Route> routes = routes(request).stream()
					.map(r -> Route.builder().route(r).build())
					.collect(Collectors.toSet());
			manifest.routes(routes);
		}

		if(getDockerImage(request) != null){
			manifest.docker(Docker.builder().image(getDockerImage(request)).build());
		} else {
			manifest.buildpack(buildpack(request));
		}
		return manifest.build();
	}


	private DeploymentState mapShallowAppState(ApplicationSummary applicationSummary) {
		if (applicationSummary.getRunningInstances().equals(applicationSummary.getInstances())) {
			return DeploymentState.deployed;
		}
		else if (applicationSummary.getInstances() > 0) {
			return DeploymentState.partial;
		} else {
			return DeploymentState.undeployed;
		}
	}

	@Override
	public AppStatus status(String id) {
		try {
			return getStatus(id)
					.doOnSuccess(v -> logger.info("Successfully computed status [{}] for {}", v, id))
					.doOnError(logError(String.format("Failed to compute status for %s", id)))
					.block(Duration.ofMillis(this.deploymentProperties.getStatusTimeout()));
		}
		catch (Exception timeoutDueToBlock) {
			logger.error("Caught exception while querying for status of {}", id, timeoutDueToBlock);
			return createErrorAppStatus(id);
		}
	}

	@Override
	public Mono<String> undeploy(String id) {
		getStatus(id)
				.doOnNext(status -> assertApplicationExists(id, status))
				// Need to block here to be able to throw exception early
				.block(Duration.ofSeconds(this.deploymentProperties.getApiTimeout()));
		requestDeleteApplication(id)
				.timeout(Duration.ofSeconds(this.deploymentProperties.getApiTimeout()))
				.doOnSuccess(v -> logger.info("Successfully undeployed app {}", id))
				.doOnError(logError(String.format("Failed to undeploy app %s", id)))
				.subscribe();
		return Mono.just(id);
	}

	private void assertApplicationDoesNotExist(String deploymentId, AppStatus status) {
		DeploymentState state = status.getState();
		if (state != DeploymentState.unknown && state != DeploymentState.error) {
			throw new IllegalStateException(String.format("App %s is already deployed with state %s", deploymentId, state));
		}
	}

	private void assertApplicationExists(String deploymentId, AppStatus status) {
		DeploymentState state = status.getState();
		if (state == DeploymentState.unknown) {
			throw new IllegalStateException(String.format("App %s is not in a deployed state", deploymentId));
		}
	}

	private AppStatus createAppStatus(ApplicationDetail applicationDetail, String deploymentId) {
		logger.trace("Gathering instances for " + applicationDetail);
		logger.trace("InstanceDetails: " + applicationDetail.getInstanceDetails());

		AppStatus.Builder builder = AppStatus.of(deploymentId);

		int i = 0;
		for (InstanceDetail instanceDetail : applicationDetail.getInstanceDetails()) {
			builder.with(new CloudFoundryAppInstanceStatus(applicationDetail, instanceDetail, i++));
		}
		for (; i < applicationDetail.getInstances(); i++) {
			builder.with(new CloudFoundryAppInstanceStatus(applicationDetail, null, i));
		}

		return builder.build();
	}

	private AppStatus createEmptyAppStatus(String deploymentId) {
		return AppStatus.of(deploymentId)
				.build();
	}

	private AppStatus createErrorAppStatus(String deploymentId) {
		return AppStatus.of(deploymentId)
				.generalState(DeploymentState.error)
				.build();
	}

	private String deploymentId(AppDeploymentRequest request) {
		String prefix = Optional.ofNullable(request.getDeploymentProperties().get(GROUP_PROPERTY_KEY))
				.map(group -> String.format("%s-", group))
				.orElse("");

		String appName = String.format("%s%s", prefix, request.getDefinition().getName());

		return this.applicationNameGenerator.generateAppName(appName);
	}

	private String domain(AppDeploymentRequest request) {
		return Optional.ofNullable(request.getDeploymentProperties().get(DOMAIN_PROPERTY))
				.orElse(this.deploymentProperties.getDomain());
	}

	private Map<String, String> getApplicationProperties(String deploymentId, AppDeploymentRequest request) {
		Map<String, String> applicationProperties = getSanitizedApplicationProperties(deploymentId, request);

		if (!useSpringApplicationJson(request)) {
			return applicationProperties;
		}

		try {
			return Collections.singletonMap("SPRING_APPLICATION_JSON", OBJECT_MAPPER.writeValueAsString(applicationProperties));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, String> getCommandLineArguments(AppDeploymentRequest request) {
		if (request.getCommandlineArguments().isEmpty()) {
			return Collections.emptyMap();
		}

		String argumentsAsString = request.getCommandlineArguments().stream()
				.collect(Collectors.joining(" "));
		String yaml = new Yaml().dump(Collections.singletonMap("arguments", argumentsAsString));

		return Collections.singletonMap("JBP_CONFIG_JAVA_MAIN", yaml);
	}

	private Map<String, String> getEnvironmentVariables(String deploymentId, AppDeploymentRequest request) {
		Map<String, String> envVariables = new HashMap<>();
		envVariables.putAll(getApplicationProperties(deploymentId, request));
		envVariables.putAll(getCommandLineArguments(request));
		String javaOpts = javaOpts(request);
		if (StringUtils.hasText(javaOpts)) {
			envVariables.put("JAVA_OPTS", javaOpts(request));
		}
		String group = request.getDeploymentProperties().get(ReactiveAppDeployer.GROUP_PROPERTY_KEY);
		if (StringUtils.hasText(group)) {
			envVariables.put("SPRING_CLOUD_APPLICATION_GROUP", group);
		}
		envVariables.put("SPRING_CLOUD_APPLICATION_GUID", "${vcap.application.name}:${vcap.application.instance_index}");
		envVariables.put("SPRING_APPLICATION_INDEX", "${vcap.application.instance_index}");
		return envVariables;
	}

	private Map<String, String> getSanitizedApplicationProperties(String deploymentId, AppDeploymentRequest request) {
		Map<String, String> applicationProperties = new HashMap<>(request.getDefinition().getProperties());

		// Remove server.port as CF assigns a port for us, and we don't want to override that
		Optional.ofNullable(applicationProperties.remove("server.port"))
				.ifPresent(port -> logger.warn("Ignoring 'server.port={}' for app {}, as Cloud Foundry will assign a local dynamic port. Route to the app will use port 80.", port, deploymentId));

		return applicationProperties;
	}

	private Mono<AppStatus> getStatus(String deploymentId) {
		return requestGetApplication(deploymentId)
				.map(applicationDetail -> createAppStatus(applicationDetail, deploymentId))
				.onErrorResume(IllegalArgumentException.class, t -> {
					logger.debug("Application for {} does not exist.", deploymentId);
					return Mono.just(createEmptyAppStatus(deploymentId));
				})
				.transform(statusRetry(deploymentId))
				.onErrorReturn(createErrorAppStatus(deploymentId));
	}

	private ApplicationHealthCheck healthCheck(AppDeploymentRequest request) {
		return Optional.ofNullable(request.getDeploymentProperties().get(HEALTHCHECK_PROPERTY_KEY))
				.map(this::toApplicationHealthCheck)
				.orElse(this.deploymentProperties.getHealthCheck());
	}

	private String healthCheckEndpoint(AppDeploymentRequest request) {
		return Optional.ofNullable(request.getDeploymentProperties().get(HEALTHCHECK_HTTP_ENDPOINT_PROPERTY_KEY))
				.orElse(this.deploymentProperties.getHealthCheckHttpEndpoint());
	}

	private Integer healthCheckTimeout(AppDeploymentRequest request) {
		String timeoutString = request.getDeploymentProperties()
				.getOrDefault(HEALTHCHECK_TIMEOUT_PROPERTY_KEY, this.deploymentProperties.getHealthCheckTimeout());
		return Integer.parseInt(timeoutString);
	}

	private String host(AppDeploymentRequest request) {
		return Optional.ofNullable(request.getDeploymentProperties().get(HOST_PROPERTY))
				.orElse(this.deploymentProperties.getHost());
	}

	private int instances(AppDeploymentRequest request) {
		return Optional.ofNullable(request.getDeploymentProperties().get(ReactiveAppDeployer.COUNT_PROPERTY_KEY))
				.map(Integer::parseInt)
				.orElse(this.deploymentProperties.getInstances());
	}


	private Mono<Void> requestDeleteApplication(String id) {
		return this.operations.applications()
				.delete(DeleteApplicationRequest.builder()
						.deleteRoutes(deploymentProperties.isDeleteRoutes())
						.name(id)
						.build());
	}

	private Mono<ApplicationDetail> requestGetApplication(String id) {
		return this.operations.applications()
				.get(GetApplicationRequest.builder()
						.name(id)
						.build());
	}

	private Mono<Void> requestPushApplication(PushApplicationManifestRequest request) {
		return this.operations.applications()
				.pushManifest(request);
	}

	private Flux<ApplicationSummary> requestSummary() {
		return this.operations.applications().list();
	}

	private String routePath(AppDeploymentRequest request) {
		String routePath = request.getDeploymentProperties().get(ROUTE_PATH_PROPERTY);
		if (StringUtils.hasText(routePath) && !routePath.startsWith("/")) {
			throw new IllegalArgumentException(
					"Cloud Foundry routes must start with \"/\". Route passed = [" + routePath + "].");
		}
		return routePath;
	}

	private String route(AppDeploymentRequest request) {
		return request.getDeploymentProperties().get(ROUTE_PROPERTY);
	}

	private Set<String> routes(AppDeploymentRequest request) {
		Set<String> routes = new HashSet<>();
		routes.addAll(this.deploymentProperties.getRoutes());
		routes.addAll(StringUtils.commaDelimitedListToSet(request.getDeploymentProperties().get(ROUTES_PROPERTY)));
		return routes;
	}

	private ApplicationHealthCheck toApplicationHealthCheck(String raw) {
		try {
			return ApplicationHealthCheck.from(raw);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(String.format("Unsupported health-check value '%s'. Available values are %s", raw,
					StringUtils.arrayToCommaDelimitedString(ApplicationHealthCheck.values())), e);
		}
	}

	private Boolean toggleNoRoute(AppDeploymentRequest request) {
		return Optional.ofNullable(request.getDeploymentProperties().get(NO_ROUTE_PROPERTY))
				.map(Boolean::valueOf)
				.orElse(null);
	}

	private boolean useSpringApplicationJson(AppDeploymentRequest request) {
		return Optional.ofNullable(request.getDeploymentProperties().get(USE_SPRING_APPLICATION_JSON_KEY))
				.map(Boolean::valueOf)
				.orElse(this.deploymentProperties.isUseSpringApplicationJson());
	}

}