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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.cloudfoundry.operations.applications.ApplicationHealthCheck;

import org.springframework.cloud.appbroker.deployer.DeploymentProperties;

/**
 * Holds configuration properties for specifying what resources and services an app deployed to a Cloud Foundry runtime
 * will get.
 *
 * @author Eric Bottard
 * @author Greg Turnquist
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings({"unused", "PMD.TooManyFields"})
public class CloudFoundryDeploymentProperties extends DeploymentProperties {

	/**
	 * Key for storing the health check deployment property
	 */
	protected static final String HEALTHCHECK_PROPERTY_KEY = "health-check";

	/**
	 * Key for storing the health check endpoint deployment property
	 */
	protected static final String HEALTHCHECK_HTTP_ENDPOINT_PROPERTY_KEY = "health-check-http-endpoint";

	/**
	 * Key for storing the health check timeout deployment property
	 */
	protected static final String HEALTHCHECK_TIMEOUT_PROPERTY_KEY = "health-check-timeout";

	/**
	 * Key for storing the api completion timeout property in seconds.
	 */
	protected static final String API_POLLING_TIMEOUT_PROPERTY_KEY = "api-polling-timeout";

	/**
	 * Key for storing the route path deployment property
	 */
	protected static final String ROUTE_PATH_PROPERTY = "route-path";

	/**
	 * Key for storing the routes deployment property
	 */
	protected static final String ROUTES_PROPERTY = "routes";

	/**
	 * Key for storing a property describing whether routes are defined
	 */
	protected static final String NO_ROUTE_PROPERTY = "no-route";

	/**
	 * Key for storing the domain deployment property
	 */
	protected static final String DOMAIN_PROPERTY = "domain";

	/**
	 * Key for storing the domains deployment property
	 */
	protected static final String DOMAINS_PROPERTY = "domains";

	/**
	 * Key for storing the buildpack deployment property
	 */
	protected static final String BUILDPACK_PROPERTY_KEY = "buildpack";

	/**
	 * Key for storing JAVA_OPTS deployment property
	 */
	protected static final String JAVA_OPTS_PROPERTY_KEY = "javaOpts";

	/**
	 * The default value for the  {@link #apiPollingTimeout} property.
	 * Useful in unit tests to assert default value when not assigned specifically.
	 */
	public static final long DEFAULT_API_POLLING_TIMEOUT_SECONDS = Duration.ofMinutes(5).getSeconds();

	/**
	 * The domain to use when mapping routes for applications.
	 */
	private String domain;

	/**
	 * The list of domain to use when mapping routes for applications.
	 */
	private Set<String> domains = new HashSet<>();

	/**
	 * The routes that the application should be bound to. Mutually exclusive with host and domain.
	 */
	private Set<String> routes = new HashSet<>();

	/**
	 * The buildpack to use for deploying the application.
	 */
	private String buildpack = "";

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
	 * Flag to enable prefixing the app name with a random prefix.
	 */
	private boolean enableRandomAppNamePrefix = true;

	/**
	 * Timeout for blocking CF API calls, in seconds.
	 */
	private long apiTimeout = 360L;

	/**
	 * Timeout for polled async CF API calls, in seconds. Named "completionTimeout" in cf-java-client
	 */
	private long apiPollingTimeout = DEFAULT_API_POLLING_TIMEOUT_SECONDS;

	/**
	 * Timeout for name API operations in milliseconds
	 */
	private long statusTimeout = 5_000L;

	/**
	 * If set, override the timeout allocated for staging the app by the client.
	 */
	private Duration stagingTimeout = Duration.ofMinutes(15L);

	/**
	 * If set, override the timeout allocated for starting the app by the client.
	 */
	private Duration startupTimeout = Duration.ofMinutes(5L);

	/**
	 * Whether to also delete routes when un-deploying an application.
	 */
	private boolean deleteRoutes = true;

	private String javaOpts;

	public String getBuildpack() {
		return buildpack;
	}

	public void setBuildpack(String buildpack) {
		this.buildpack = buildpack;
	}

	public boolean isEnableRandomAppNamePrefix() {
		return enableRandomAppNamePrefix;
	}

	public void setEnableRandomAppNamePrefix(boolean enableRandomAppNamePrefix) {
		this.enableRandomAppNamePrefix = enableRandomAppNamePrefix;
	}

	public long getApiTimeout() {
		return apiTimeout;
	}

	public void setApiTimeout(long apiTimeout) {
		this.apiTimeout = apiTimeout;
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

	public Set<String> getDomains() {
		return domains;
	}

	public void setDomains(Set<String> domains) {
		this.domains = domains;
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

	public long getApiPollingTimeout() {
		return apiPollingTimeout;
	}

	public void setApiPollingTimeout(long apiPollingTimeout) {
		this.apiPollingTimeout = apiPollingTimeout;
	}


}
