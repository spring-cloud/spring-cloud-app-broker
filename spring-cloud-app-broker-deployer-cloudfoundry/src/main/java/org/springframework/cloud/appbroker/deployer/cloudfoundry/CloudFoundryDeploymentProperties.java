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

import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds configuration properties for specifying what resources and services an app deployed to a Cloud Foundry runtime
 * will get.
 *
 * @author Eric Bottard
 * @author Greg Turnquist
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings({"unused", "PMD.TooManyFields"})
public class CloudFoundryDeploymentProperties {
	static final String HEALTHCHECK_PROPERTY_KEY = "health-check";

	static final String HEALTHCHECK_HTTP_ENDPOINT_PROPERTY_KEY = "health-check-http-endpoint";

	static final String HEALTHCHECK_TIMEOUT_PROPERTY_KEY = "health-check-timeout";

	static final String ROUTE_PATH_PROPERTY = "route-path";

	static final String ROUTE_PROPERTY = "route";

	static final String ROUTES_PROPERTY = "routes";

	static final String NO_ROUTE_PROPERTY = "no-route";

	static final String HOST_PROPERTY = "host";

	static final String DOMAIN_PROPERTY = "domain";

	static final String BUILDPACK_PROPERTY_KEY = "buildpack";

	static final String JAVA_OPTS_PROPERTY_KEY = "javaOpts";

	static final String USE_SPRING_APPLICATION_JSON_KEY = "use-spring-application-json";

	/**
	 * The host name to use as part of the route. Defaults to hostname derived by Cloud Foundry.
	 */
	private String host;

	/**
	 * The domain to use when mapping routes for applications.
	 */
	private String domain;

	/**
	 * The routes that the application should be bound to.
	 * Mutually exclusive with host and domain.
	 */
	private Set<String> routes = new HashSet<>();

	/**
	 * The buildpack to use for deploying the application.
	 */
	private String buildpack;

	/**
	 * The amount of memory to allocate, if not overridden per-app. Default unit is mebibytes, 'M' and 'G" suffixes supported.
	 */
	private String memory;

	/**
	 * The amount of disk space to allocate, if not overridden per-app. Default unit is mebibytes, 'M' and 'G" suffixes supported.
	 */
	private String disk;

	/**
	 * The type of health check to perform on deployed application, if not overridden per-app.  Defaults to PORT
	 */
	private ApplicationHealthCheck healthCheck;

	/**
	 * The path that the http health check will use, defaults to @{code /health}
	 */
	private String healthCheckHttpEndpoint;

	/**
	 * The timeout value for health checks in seconds.  Defaults to 120 seconds.
	 */
	private Integer healthCheckTimeout;

	/**
	 * The number of instances to run.
	 */
	private Integer instances;

	/**
	 * Flag to enable prefixing the app name with a random prefix.
	 */
	private boolean enableRandomAppNamePrefix = true;

	/**
	 * Timeout for blocking API calls, in seconds.
	 */
	private long apiTimeout = 360L;

	/**
	 * Timeout for name API operations in milliseconds
	 */
	private long statusTimeout = 5_000L;

	/**
	 * Flag to indicate whether application properties are fed into SPRING_APPLICATION_JSON or ENVIRONMENT VARIABLES.
	 */
	private boolean useSpringApplicationJson = true;

	/**
	 * If set, override the timeout allocated for staging the app by the client.
	 */
	private Duration stagingTimeout = Duration.ofMinutes(15L);

	/**
	 * If set, override the timeout allocated for starting the app by the client.
	 */
	private Duration startupTimeout = Duration.ofMinutes(5L);

	/**
	 * String to use as prefix for name of deployed app.  Defaults to spring.application.name.
	 */
	@Value("${spring.application.name:}")
	private String appNamePrefix;

	/**
	 * Whether to also delete routes when un-deploying an application.
	 */
	private boolean deleteRoutes = true;

	private String javaOpts;

	private String defaultOrg;

	private String username;

	public String getBuildpack() {
		return buildpack;
	}

	public void setBuildpack(String buildpack) {
		this.buildpack = buildpack;
	}

	public String getMemory() {
		return memory;
	}

	public void setMemory(String memory) {
		this.memory = memory;
	}

	public String getDisk() {
		return disk;
	}

	public void setDisk(String disk) {
		this.disk = disk;
	}

	public Integer getInstances() {
		return instances;
	}

	public void setInstances(int instances) {
		this.instances = instances;
	}

	public boolean isEnableRandomAppNamePrefix() {
		return enableRandomAppNamePrefix;
	}

	public void setEnableRandomAppNamePrefix(boolean enableRandomAppNamePrefix) {
		this.enableRandomAppNamePrefix = enableRandomAppNamePrefix;
	}

	public String getAppNamePrefix() {
		return appNamePrefix;
	}

	public void setAppNamePrefix(String appNamePrefix) {
		this.appNamePrefix = appNamePrefix;
	}

	public long getApiTimeout() {
		return apiTimeout;
	}

	public void setApiTimeout(long apiTimeout) {
		this.apiTimeout = apiTimeout;
	}

	public boolean isUseSpringApplicationJson() {
		return useSpringApplicationJson;
	}

	public void setUseSpringApplicationJson(boolean useSpringApplicationJson) {
		this.useSpringApplicationJson = useSpringApplicationJson;
	}

	public ApplicationHealthCheck getHealthCheck() {
		return healthCheck;
	}

	public void setHealthCheck(ApplicationHealthCheck healthCheck) {
		this.healthCheck = healthCheck;
	}

	public String getHealthCheckHttpEndpoint() {
		return healthCheckHttpEndpoint;
	}

	public void setHealthCheckHttpEndpoint(String healthCheckHttpEndpoint) {
		this.healthCheckHttpEndpoint = healthCheckHttpEndpoint;
	}

	public Integer getHealthCheckTimeout() {
		return healthCheckTimeout;
	}

	public void setHealthCheckTimeout(Integer healthCheckTimeout) {
		this.healthCheckTimeout = healthCheckTimeout;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Set<String> getRoutes() {
		return routes;
	}

	public void setRoutes(Set<String> routes) {
		this.routes = routes;
	}

	public Duration getStagingTimeout() {
		return stagingTimeout;
	}

	public void setStagingTimeout(Duration stagingTimeout) {
		this.stagingTimeout = stagingTimeout;
	}

	public Duration getStartupTimeout() {
		return startupTimeout;
	}

	public void setStartupTimeout(Duration startupTimeout) {
		this.startupTimeout = startupTimeout;
	}

	public long getStatusTimeout() {
		return statusTimeout;
	}

	public void setStatusTimeout(long statusTimeout) {
		this.statusTimeout = statusTimeout;
	}

	public boolean isDeleteRoutes() {
		return deleteRoutes;
	}

	public void setDeleteRoutes(boolean deleteRoutes) {
		this.deleteRoutes = deleteRoutes;
	}

	public String getJavaOpts() {
		return javaOpts;
	}

	public void setJavaOpts(String javaOpts) {
		this.javaOpts = javaOpts;
	}

	public String getDefaultOrg() {
		return defaultOrg;
	}

	public void setDefaultOrg(String defaultOrg) {
		this.defaultOrg = defaultOrg;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
